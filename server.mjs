import {createReadStream} from 'node:fs';
import {createServer} from 'node:http';
import {pipeline} from 'node:stream/promises';

const prefix = '/api/embed/v2/';
const forwardedPrefix = prefix.slice(0, -1);
const geo = 'https://geo.so.ch';
const dataproductPath = 'api/dataproduct/v1/';
const documentPath = 'api/v1/document/';
const mapPath = 'ows/somap';
const identifyPath = 'api/v1/featureinfo/somap';
const backgrounds = new Set(['hintergrundkarte_sw', 'hintergrundkarte_farbig', 'hintergrundkarte_ortho']);
const tiles = new RegExp(String.raw`^api/wmts/1\.0\.0/ch\.so\.agi\.(?:${[...backgrounds].join('|')})/default/2056/\d+/\d+/\d+\.png$`);
const mapQuery = new Set('service request version layers styles format transparent width height bbox crs srs dpi format_options map_resolution opacities tiled __t'.split(' '));
const identifyQuery = new Set([...mapQuery, ...'query_layers info_format i j x y feature_count radius fi_line_tolerance fi_point_tolerance fi_polygon_tolerance with_geometry with_maptip with_htmlcontent with_displayname id filter'.split(' ')]);
// Preserve the legacy Spring Boot probe URLs.
const healthPaths = new Set([
    '/actuator/health',
    '/actuator/health/livenessState',
    '/actuator/health/readinessState'
]);
const viewerPolicy = [
    "default-src 'none'",
    "script-src 'self'",
    "style-src 'self' 'unsafe-inline'",
    "font-src 'self' data:",
    "img-src 'self' data: blob: http: https:",
    "connect-src 'self'",
    "frame-src about:",
    'frame-ancestors *',
    "base-uri 'none'",
    "form-action 'none'"
].join('; ');
const staticFiles = {
    'index.html': ['index.html', 'text/html; charset=utf-8'],
    'viewer.js': ['dist/viewer.js', 'text/javascript'],
    'viewer.css': ['dist/viewer.css', 'text/css']
};

function configForEmbed(config) {
    const identify = config.plugins.desktop.find(plugin => plugin.name === 'Identify');
    const plugins = [{name: 'Map'}, {
        ...identify,
        cfg: {
            ...identify.cfg,
            clearResultsOnClose: true,
            // Match --full-map-clearance in viewer.css.
            geometry: {initialHeight: 220, initialWidth: 320, initialX: 0, initialY: 40}
        }
    }];
    for (const key of Object.keys(config)) {
        if (key.endsWith('Service') || key.endsWith('ServiceUrl')) delete config[key];
    }
    return {
        ...config,
        plugins: {desktop: plugins, mobile: plugins},
        assetsPath: prefix + 'assets',
        translationsPath: prefix + 'translations',
        dataproductServiceUrl: prefix + dataproductPath,
        documentServiceUrl: prefix + documentPath,
        username: null,
        user_infos: {},
        allowFractionalZoom: true,
        // These public services are exposed by the proxy as GET routes.
        wmsMaxGetUrlLength: Number.MAX_SAFE_INTEGER
    };
}

function themesForEmbed({themes}) {
    const theme = themes.items.find(item => item.id === themes.defaultTheme);
    if (theme.url !== '/' + mapPath || theme.featureInfoUrl !== '/' + identifyPath) {
        throw new Error('Unexpected SO!GIS theme service URLs');
    }
    theme.url = prefix + mapPath;
    theme.featureInfoUrl = prefix + identifyPath;
    delete theme.editConfigUrl;
    theme.bbox.bounds = [2420000, 1030000, 2900000, 1350000];
    themes.items = [theme];
    themes.subdirs = [];
    themes.backgroundLayers = themes.backgroundLayers.filter(layer => backgrounds.has(layer.name));
    for (const layer of themes.backgroundLayers) {
        const expectedPath = `api/wmts/1.0.0/ch.so.agi.${layer.name}/default/{TileMatrixSet}/{TileMatrix}/{TileRow}/{TileCol}.png`;
        if (layer.url !== geo + '/' + expectedPath || layer.tileMatrixSet !== '2056') {
            throw new Error('Unexpected SO!GIS WMTS URL or tile matrix set');
        }
        layer.url = prefix + expectedPath;
    }
    const overview = themes.backgroundLayers.find(layer => layer.name === 'hintergrundkarte_sw');
    if (!overview || themes.backgroundLayers.length !== backgrounds.size) throw new Error('Missing SO!GIS background layers');
    // QWC2 uses a 96 dpi screen. The WMTS grid covers the former viewer's zoom range.
    theme.scales = overview.resolutions.map(resolution => resolution * 96 / 0.0254);
    return {themes};
}

function serviceQuery(route, parameters, tile) {
    if (tile) return parameters.size ? null : '';
    if (route === dataproductPath + 'weblayers') {
        return parameters.size === 1 && parameters.get('filter') ? '?' + parameters : null;
    }
    const allowed = route === mapPath ? mapQuery : identifyQuery;
    const request = route === mapPath ? 'getmap' : 'getfeatureinfo';
    const seen = new Set();
    for (const [key, value] of parameters) {
        const name = key.toLowerCase();
        if (!allowed.has(name) || seen.has(name)) return null;
        if (name === 'request' && value.toLowerCase() !== request) return null;
        if (name === 'id' && value !== 'default') return null;
        if (name === 'filter' && value) return null;
        seen.add(name);
    }
    return seen.has('request') ? '?' + parameters : null;
}

async function upstream(path) {
    return fetch(geo + '/' + path, {redirect: 'error', signal: AbortSignal.timeout(20_000)});
}

export default createServer(async (req, res) => {
    try {
        let requestTarget = req.url;
        let url = new URL(requestTarget, 'http://localhost');
        if (req.method === 'GET' && url.pathname === '/ping') {
            return res.writeHead(200, {'Content-Type': 'text/plain; charset=utf-8'}).end('pong');
        }
        if (req.method === 'GET' && healthPaths.has(url.pathname)) {
            return res.writeHead(200, {'Content-Type': 'application/json'}).end('{"status":"UP"}');
        }
        if (req.headers['x-forwarded-prefix'] === forwardedPrefix &&
            requestTarget.startsWith('/') && !requestTarget.startsWith('//') &&
            !requestTarget.startsWith(prefix) && !url.pathname.startsWith(prefix)) {
            requestTarget = forwardedPrefix + requestTarget;
            url = new URL(requestTarget, 'http://localhost');
        }
        if (!url.pathname.startsWith(prefix)) return res.writeHead(404).end();
        if (req.method !== 'GET') return res.writeHead(405, {Allow: 'GET'}).end();
        const route = url.pathname.slice(prefix.length);
        res.setHeader('X-Content-Type-Options', 'nosniff');
        const rawPath = requestTarget.split('?', 1)[0];
        const documentRoot = prefix + documentPath.slice(0, -1);
        if (rawPath === documentRoot || rawPath.startsWith(documentRoot + '/')) {
            // Reject path delimiters and dot segments that another decoder could normalize.
            if (rawPath !== url.pathname || /%(?:25)*(?:2f|5c)|(?:^|\/)(?:\.|%(?:25)*2e){1,2}(?=\/|$)/i.test(rawPath)) {
                return res.writeHead(404).end();
            }
            const source = await upstream(route + requestTarget.slice(rawPath.length));
            for (const header of ['Content-Type', 'Content-Disposition']) {
                const value = source.headers.get(header);
                if (value !== null) res.setHeader(header, value);
            }
            res.setHeader('Cache-Control', 'no-store');
            res.setHeader('Content-Security-Policy', "sandbox; default-src 'none'");
            res.writeHead(source.status);
            if (source.body) await pipeline(source.body, res);
            else res.end();
            return;
        }
        if (Object.hasOwn(staticFiles, route)) {
            if (route === 'index.html') {
                res.setHeader('Content-Security-Policy', viewerPolicy);
                res.setHeader('Referrer-Policy', 'no-referrer');
            }
            const [file, contentType] = staticFiles[route];
            res.setHeader('Content-Type', contentType);
            return await pipeline(createReadStream(new URL(file, import.meta.url)), res);
        }
        if (route === 'config.json' || route === 'themes.json') {
            const source = await upstream('map/' + route);
            if (!source.ok) throw new Error(`SO!GIS returned HTTP ${source.status} for ${route}`);
            const data = await source.json();
            const config = route === 'config.json' ? configForEmbed(data) : themesForEmbed(data);
            return res.writeHead(200, {'Content-Type': 'application/json'}).end(JSON.stringify(config));
        }
        const tile = tiles.test(route);
        if (route !== mapPath && route !== identifyPath && route !== dataproductPath + 'weblayers' && !tile) {
            return res.writeHead(404).end();
        }
        const query = serviceQuery(route, url.searchParams, tile);
        if (query === null) return res.writeHead(400).end();
        const source = await upstream(route + query);
        const contentType = source.headers.get('content-type')?.split(';')[0].trim();
        const expectedTypes = route === identifyPath ? ['text/xml', 'application/xml'] :
            route === dataproductPath + 'weblayers' ? ['application/json'] : ['image/png', 'image/jpeg'];
        if (!source.ok || !expectedTypes.includes(contentType)) {
            await source.body?.cancel();
            return res.writeHead(source.ok ? 502 : source.status, {'Content-Type': 'text/plain; charset=utf-8'})
                .end('Der öffentliche SO!GIS-Dienst konnte die Anfrage nicht beantworten.');
        }
        res.writeHead(source.status, {'Content-Type': source.headers.get('content-type')});
        if (source.body) await pipeline(source.body, res);
        else res.end();
    } catch (error) {
        if (res.destroyed) return;
        console.error(error);
        if (res.headersSent) return res.destroy();
        res.writeHead(502, {'Content-Type': 'text/plain; charset=utf-8'}).end('SO!GIS ist vorübergehend nicht verfügbar.');
    }
}).listen(Number(process.env.PORT ?? 8080));
