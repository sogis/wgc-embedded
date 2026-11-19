import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import {createServer, get} from 'node:http';
import {once} from 'node:events';
import {test} from 'node:test';
import {chromium} from 'playwright';

const embed = new URL('/api/embed/v2/index.html', process.env.EMBED_ORIGIN ?? 'http://localhost:8080');
const forwardedPrefixHeaders = {'X-Forwarded-Prefix': '/api/embed/v2'};

function rawStatus(origin, path, headers) {
    return new Promise((resolve, reject) => {
        get(origin, {path, headers}, response => {
            response.resume();
            resolve(response.statusCode);
        }).on('error', reject);
    });
}

test('Der Container bedient die bisherigen Health-Probes und die Embed-URL', async () => {
    for (const path of ['/actuator/health', '/actuator/health/livenessState', '/actuator/health/readinessState']) {
        const response = await fetch(new URL(path, embed), {redirect: 'manual', signal: AbortSignal.timeout(5000)});
        assert.equal(response.status, 200, path);
        assert.match(response.headers.get('content-type'), /^application\/json\b/);
        assert.deepEqual(await response.json(), {status: 'UP'}, path);
    }

    const ping = await fetch(new URL('/ping', embed), {redirect: 'manual', signal: AbortSignal.timeout(5000)});
    assert.equal(ping.status, 200);
    assert.match(ping.headers.get('content-type'), /^text\/plain\b/);
    assert.equal(await ping.text(), 'pong');

    const index = await fetch(embed, {redirect: 'manual', signal: AbortSignal.timeout(5000)});
    assert.equal(index.status, 200);
    assert.match(index.headers.get('content-type'), /^text\/html\b/);
    assert.match(await index.text(), /<title>SO!GIS Karte<\/title>/);
});

test('Der Container akzeptiert gekürzte Embed-Pfade mit dem festen nginx-Header', async () => {
    const direct = await fetch(embed);
    assert.equal(direct.status, 200);
    const html = await direct.text();
    for (const path of ['/index.html', embed.pathname]) {
        const response = await fetch(new URL(path, embed), {headers: forwardedPrefixHeaders, redirect: 'manual'});
        assert.equal(response.status, 200, path);
        assert.equal(await response.text(), html, path);
    }

    for (const headers of [{}, {'X-Forwarded-Prefix': '/api/embed/v2/'},
        {'X-Forwarded-Prefix': ['/api/embed/v2', '/api/embed/v2']}]) {
        assert.equal(await rawStatus(embed.origin, '/index.html', headers), 404);
    }
    for (const path of ['//localhost/index.html', 'http://localhost/index.html', '/unknown.html']) {
        assert.equal(await rawStatus(embed.origin, path, forwardedPrefixHeaders), 404, path);
    }
    const response = await fetch(new URL('/config.json', embed), {method: 'POST', headers: forwardedPrefixHeaders});
    assert.equal(response.status, 405);
    assert.equal(response.headers.get('allow'), 'GET');
    for (const path of ['/ping', '/actuator/health', '/actuator/health/livenessState', '/actuator/health/readinessState']) {
        assert.equal(await rawStatus(embed.origin, path, forwardedPrefixHeaders), 200, path);
    }
});

test('Die Embed-Seite zeichnet Live-WMTS und dynamische SO!GIS-Ebenen', async t => {
    const browser = await chromium.launch();
    t.after(() => browser.close());
    const page = await browser.newPage({viewport: {width: 600, height: 390}});
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    const unexpected = [];
    page.on('request', request => {
        if (new URL(request.url()).origin !== embed.origin) unexpected.push(request.url());
    });
    const assets = ['viewer.js', 'viewer.css'].map(file =>
        page.waitForResponse(response => response.ok() && response.url() === new URL(file, embed).href));
    const orthophoto = page.waitForResponse(response => response.ok() && response.url().includes('/ch.so.agi.hintergrundkarte_ortho/'));
    const mapImage = page.waitForResponse(response => response.ok() && response.url().includes('/ows/somap?'));
    const weblayers = page.waitForResponse(response => response.ok() && response.url().includes('/weblayers?'));
    const url = new URL(embed);
    url.search = new URLSearchParams({bl: 'ch.so.agi.hintergrundkarte_ortho', l: 'ch.so.agi.av.grundstuecke', c: '2607739,1228426', s: '2000'});
    await page.goto(url.href);
    const [tile, map, layers] = await Promise.all([orthophoto, mapImage, weblayers, ...assets]);
    for (const response of [tile, map]) {
        const image = await response.body();
        const signature = image.subarray(0, 8).toString('hex');
        assert.ok(signature === '89504e470d0a1a0a' || signature.startsWith('ffd8ff'), signature);
    }
    assert.equal(new URL(map.url()).searchParams.get('LAYERS'), 'ch.so.agi.av.grundstuecke');
    assert.ok((await layers.json())['ch.so.agi.av.grundstuecke'].length);
    await page.waitForFunction(() => {
        const canvas = document.querySelector('.ol-layer canvas');
        return canvas?.getContext('2d').getImageData(300, 195, 1, 1).data[3] === 255;
    });
    assert.equal(new URL(page.url()).pathname, embed.pathname);
    await page.waitForLoadState('networkidle');
    const canvas = page.locator('.ol-layer canvas');
    const withParcel = await canvas.screenshot();
    url.searchParams.delete('l');
    await page.goto(url.href);
    await page.waitForLoadState('networkidle');
    assert.notDeepEqual(await canvas.screenshot(), withParcel, 'Das Grundstückslayer verändert das sichtbare Kartenbild');
    assert.deepEqual(errors, []);
    assert.deepEqual(unexpected, []);
});

test('Der Embed-Prefix gewährt nur Zugriff auf freigegebene Lese-Endpunkte', async () => {
    const cases = [
        ['../v1/index.html', 404],
        ['%2e%2e/%2e%2e/map/config.json', 404],
        ['api/v1/landreg/', 404],
        ['https://example.org/', 404],
        ['ows/somap?REQUEST=GetMap&MAP=other', 400],
        ['ows/somap?REQUEST=GetCapabilities', 400],
        ['ows/somap?REQUEST=GetMap&request=GetMap', 400],
        ['api/v1/featureinfo/somap?request=GetFeatureInfo&filter=other', 400],
        ['api/v1/document/%2e%2e%2fother.pdf', 404],
        ['api/v1/documentation/foo.xlsx', 404],
        ['api/v1/document/foo.xlsx', 405, 'POST'],
        ['config.json', 405, 'POST']
    ];
    for (const [path, status, method] of cases) {
        const response = await fetch(new URL('/api/embed/v2/' + path, embed), {method, redirect: 'manual'});
        assert.equal(response.status, status, path);
    }
});

test('Der Dokument-Proxy reicht Unterpfad, Query, Formate und Upstream-Fehler durch', async t => {
    const clientFetch = globalThis.fetch;
    const query = '?feature=abc:123&lang=de&extra=%2F%26%2B+%C3%A4&crs=EPSG%3A4326&lang=fr';
    const xlsxType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    const disposition = "attachment; filename*=UTF-8''Auswertung%20SO.xlsx";
    const bytes = Uint8Array.of(0x50, 0x4b, 0x03, 0x04, 0x00, 0xff);
    const cases = [
        ['foo%20bar/report.xlsx' + query, new Response(bytes, {
            headers: {'Content-Type': xlsxType, 'Content-Disposition': disposition, 'Set-Cookie': 'example=value'}
        }), 200],
        ['missing', new Response('Report fehlt', {status: 404}), 404],
        ['invalid', new Response('Parameter fehlen', {status: 400}), 400],
        ['unreachable', new Error('Simulierter Verbindungsfehler'), 502]
    ];
    let nextResponse = 0;
    const upstreamFetch = t.mock.method(globalThis, 'fetch', async () => {
        const [, response] = cases[nextResponse++ % cases.length];
        if (response instanceof Error) throw response;
        return response.clone();
    });
    const logError = t.mock.method(console, 'error', () => {});
    const port = process.env.PORT;
    process.env.PORT = '0';
    const {default: server} = await import('../server.mjs').finally(() => {
        if (port === undefined) delete process.env.PORT;
        else process.env.PORT = port;
    });
    await once(server, 'listening');
    t.after(() => server.close());
    const origin = 'http://127.0.0.1:' + server.address().port;
    const variants = [
        ['/api/embed/v2/api/v1/document/', {}],
        ['/api/v1/document/', forwardedPrefixHeaders]
    ];
    for (const [reportPath, headers] of variants) {
        const callsBefore = upstreamFetch.mock.callCount();
        for (const path of ['../featureinfo/somap', '%252e%252e/other', 'folder%252f..%252fother']) {
            assert.equal(await rawStatus(origin, reportPath + path, headers), 404, reportPath + path);
        }
        assert.equal(upstreamFetch.mock.callCount(), callsBefore);

        for (const [path, upstream, status] of cases) {
            const response = await clientFetch(origin + reportPath + path, {
                headers: {...headers, Cookie: 'session=synthetic', Authorization: 'Bearer synthetic'}
            });
            assert.equal(response.status, status, path);
            if (status === 200) {
                assert.equal(response.headers.get('Content-Type'), xlsxType);
                assert.equal(response.headers.get('Content-Disposition'), disposition);
                assert.equal(response.headers.get('Set-Cookie'), null);
                assert.match(response.headers.get('Content-Security-Policy'), /^sandbox;/);
                assert.deepEqual(new Uint8Array(await response.arrayBuffer()), bytes);
            } else if (status === 502) {
                assert.match(await response.text(), /vorübergehend nicht verfügbar/);
            } else {
                assert.equal(response.headers.get('Content-Type'), upstream.headers.get('Content-Type'));
                assert.equal(response.headers.get('Content-Disposition'), null);
                assert.equal(await response.text(), status === 404 ? 'Report fehlt' : 'Parameter fehlen');
            }
        }
    }
    assert.equal(upstreamFetch.mock.callCount(), cases.length * variants.length);
    for (const [index, {arguments: [target, options]}] of upstreamFetch.mock.calls.entries()) {
        assert.equal(target, 'https://geo.so.ch/api/v1/document/' + cases[index % cases.length][0]);
        assert.equal(options.redirect, 'error');
        assert.equal(options.headers, undefined);
        assert.equal(options.method, undefined);
    }
    assert.equal(logError.mock.callCount(), variants.length);
    for (const call of logError.mock.calls) {
        assert.equal(call.arguments[0], cases.at(-1)[1]);
    }
});

const parcel = 'ch.so.agi.av.grundstuecke';
const groundwater = 'ch.so.afu.ewsabfrage.abfrage';
const hidden = 'ch.so.afu.gewaesserschutz.schutzareale';
const zones = 'ch.so.afu.gewaesserschutz.schutzzonen';
const contaminated = 'ch.so.afu.altlasten.standorte';
const external = 'wms:https://wfs.geodienste.ch/av_0/deu#Hoheitsgrenzen';

function entry(params) {
    const url = new URL(embed);
    url.search = new URLSearchParams(params);
    return url.href;
}

function mapResponse(page) {
    return page.waitForResponse(response => response.ok() && response.url().includes('/ows/somap?'));
}

function checkExtent(response, center, scale) {
    const params = new URL(response.url()).searchParams;
    const [left, bottom, right, top] = params.get('BBOX').split(',').map(Number);
    assert.ok(Math.abs((left + right) / 2 - center[0]) < 1);
    assert.ok(Math.abs((bottom + top) / 2 - center[1]) < 1);
    const resolution = (right - left) / Number(params.get('WIDTH'));
    assert.ok(Math.abs(resolution / (scale / 3780) - 1) < 0.025, `Massstab ${scale}, Auflösung ${resolution}`);
    return params;
}

test('Vorgabewerte, unabhängige c/s, Hintergrundkarten und Ausschnitt', async t => {
    const browser = await chromium.launch();
    t.after(() => browser.close());
    const page = await browser.newPage({viewport: {width: 600, height: 390}});
    await page.goto(entry({}));
    await page.waitForFunction(() => {
        const url = new URL(location.href);
        return url.searchParams.get('bl') === 'hintergrundkarte_sw' && url.searchParams.get('s') === '378000';
    });
    const cases = [
        [{}, [2616500, 1237000], 378000, 'sw'],
        [{c: '2600779,1215505'}, [2600779, 1215505], 378000, 'farbig'],
        [{s: '22694'}, [2616500, 1237000], 22694, 'ortho'],
        [{c: '2600779,1215505', s: '22694'}, [2600779, 1215505], 22694, 'ortho'],
        [{s: '4000000'}, [2616500, 1237000], 4000000, 'sw']
    ];
    for (const [params, center, scale, background] of cases) {
        const layer = 'ch.so.agi.hintergrundkarte_' + background;
        const map = mapResponse(page);
        const backgroundTile = page.waitForResponse(response => response.ok() && response.url().includes(`/${layer}/`));
        await page.goto(entry({...params, bl: layer, l: groundwater}));
        checkExtent(await map, center, scale);
        await backgroundTile;
        await page.waitForFunction(({center, scale, background}) => {
            const query = new URL(location.href).searchParams;
            return query.get('c') === center.join(',') && query.get('s') === String(scale) &&
                query.get('bl') === 'hintergrundkarte_' + background;
        }, {center, scale, background});
    }
});

test('README-Layerliste, externer WMS und Vollkartenlink nach Verschieben und Zoomen', async t => {
    const browser = await chromium.launch();
    t.after(() => browser.close());
    const context = await browser.newContext({viewport: {width: 600, height: 390}});
    const page = await context.newPage();
    const externals = [];
    page.on('request', request => {
        if (new URL(request.url()).host === 'wfs.geodienste.ch') externals.push(request.url());
    });
    const layerList = [parcel, `${groundwater}[50]`, `${hidden}!`, zones, contaminated, `${external}[35]`].join(',');
    const map = mapResponse(page);
    const externalMap = page.waitForResponse(response => response.ok() && response.url().startsWith('https://wfs.geodienste.ch/'));
    await page.goto(entry({bl: 'ch.so.agi.hintergrundkarte_ortho', l: layerList, c: '2600779,1215505', s: '22694'}));
    const mapParams = checkExtent(await map, [2600779, 1215505], 22694);
    assert.equal(mapParams.get('LAYERS'), [parcel, groundwater, zones, contaminated].join(','));
    assert.equal(mapParams.get('OPACITIES'), '255,128,255,255');
    const image = await externalMap;
    assert.match(image.headers()['content-type'], /^image\//);
    assert.equal(new URL(image.url()).searchParams.get('LAYERS'), 'Hoheitsgrenzen');
    assert.equal(image.request().resourceType(), 'image');
    assert.equal((await image.request().allHeaders())['sec-fetch-mode'], 'no-cors');
    await page.waitForFunction(layers => new URL(location.href).searchParams.get('l') === layers, layerList);

    await page.mouse.move(270, 160);
    await page.mouse.down();
    await page.mouse.move(340, 206, {steps: 5});
    await page.mouse.up();
    await page.mouse.wheel(0, -140);
    await page.waitForFunction(() => {
        const query = new URL(location.href).searchParams;
        return query.has('s') && query.get('s') !== '22694' && query.get('c') !== '2600779,1215505';
    });

    let fullMap;
    await context.route('https://geo.so.ch/map/**', async route => {
        fullMap = new URL(route.request().url());
        await route.fulfill({status: 200, body: 'In geo.so.ch/map ansehen'});
    });
    const popup = page.waitForEvent('popup');
    await page.getByRole('link', {name: 'In geo.so.ch/map ansehen'}).click();
    await (await popup).waitForLoadState();
    const actual = new URL(page.url()).searchParams;
    assert.equal(fullMap.pathname, '/map/');
    for (const key of ['bl', 'l', 'c', 's']) assert.equal(fullMap.searchParams.get(key), actual.get(key), key);
    assert.equal(actual.get('l'), layerList);
    assert.equal(actual.get('bl'), 'hintergrundkarte_ortho');
    const identify = page.waitForResponse(response => response.ok() && response.url().includes('/featureinfo/somap?'));
    await page.mouse.click(280, 185);
    await identify;
    assert.ok(externals.length);
    assert.ok(externals.every(url => new URL(url).searchParams.get('REQUEST') === 'GetMap'));
    const reloaded = mapResponse(page);
    await page.reload();
    checkExtent(await reloaded, actual.get('c').split(',').map(Number), Number(actual.get('s')));
});

test('Fremd-Origin und kleines iframe zeigen mehrere Objektinformationen und laden das Objektblatt', async t => {
    const browser = await chromium.launch();
    t.after(() => browser.close());
    const page = await browser.newPage({
        viewport: {width: 400, height: 320},
        isMobile: true,
        hasTouch: true,
        userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Version/17.0 Mobile/15E148 Safari/604.1'
    });
    const url = entry({l: `${parcel},${groundwater}[50],${hidden}!`, c: '2607739,1228426', s: '2000'});
    const host = createServer((request, response) => {
        response.writeHead(200, {'Content-Type': 'text/html; charset=utf-8'}).end(`
            <!doctype html><meta name="viewport" content="width=device-width, initial-scale=1">
            <iframe title="Karte" width="265" height="174" src="${url.replaceAll('&', '&amp;')}"></iframe>`);
    });
    host.listen(0, '127.0.0.1');
    await once(host, 'listening');
    t.after(() => host.close());
    const map = mapResponse(page);
    const frameResponse = page.waitForResponse(response => response.url() === url);
    const parent = `http://127.0.0.1:${host.address().port}`;
    assert.notEqual(parent, embed.origin);
    await page.goto(parent);
    const headers = (await frameResponse).headers();
    assert.match(headers['content-security-policy'], /frame-ancestors \*/);
    assert.equal(headers['x-frame-options'], undefined);
    assert.equal(headers['set-cookie'], undefined);
    assert.equal(headers.location, undefined);
    await map;
    const frame = page.frameLocator('iframe');
    const identify = page.waitForResponse(response => response.ok() && response.url().includes('/featureinfo/somap?'));
    await frame.locator('.ol-viewport').tap({position: {x: 132, y: 87}});
    const response = await identify;
    const params = new URL(response.url()).searchParams;
    assert.deepEqual(new Set(params.get('query_layers').split(',')), new Set([groundwater, parcel]));
    const xml = await response.text();
    assert.match(xml, /<HtmlContent inline="1">/);
    assert.match(xml, /featurereport="grundstuecksbeschrieb\/grundstuecksbeschrieb"/);
    await frame.getByText('Bohrtiefenabfrage Erdwärmesonden', {exact: true}).waitFor();
    await frame.getByText('Grundstücke', {exact: true}).waitFor();
    await frame.getByText('2 Objekt(e)', {exact: true}).waitFor();
    const dialog = await frame.locator('.resizeable-window').boundingBox();
    const linkToFullMap = frame.getByRole('link', {name: 'In geo.so.ch/map ansehen'});
    const fullMap = await linkToFullMap.boundingBox();
    const viewport = await frame.locator('.ol-viewport').boundingBox();
    assert.ok(fullMap.x - viewport.x < 16 && fullMap.y - viewport.y < 16);
    assert.equal(await linkToFullMap.evaluate(link => getComputedStyle(link).fontWeight), '700');
    assert.ok(await linkToFullMap.locator('img').evaluate(img => img.naturalWidth > 0));
    assert.ok(fullMap.y + fullMap.height <= dialog.y);
    assert.ok(dialog.y + dialog.height <= viewport.y + viewport.height);
    assert.ok(dialog.width <= 265);
    assert.match(await frame.getByRole('link', {name: 'https://dox42.so.ch'}).getAttribute('href'), /^https:\/\/dox42\.so\.ch\//);
    const link = frame.getByRole('link', {name: 'Link', exact: true});
    const reportUrl = new URL(await link.getAttribute('href'), embed);
    assert.equal(reportUrl.origin, embed.origin);
    assert.equal(reportUrl.pathname, '/api/embed/v2/api/v1/document/grundstuecksbeschrieb/grundstuecksbeschrieb.pdf');
    const download = page.waitForEvent('download');
    await link.tap();
    const report = await download;
    assert.match(report.suggestedFilename(), /grundstuecksbeschrieb\.pdf$/);
    const pdf = await readFile(await report.path());
    assert.equal(pdf.subarray(0, 5).toString(), '%PDF-');
    assert.match(pdf.subarray(-32).toString(), /%%EOF\s*$/);
    await frame.getByTitle('Schliessen', {exact: true}).tap();
    await frame.getByText('Objektinformationen', {exact: true}).waitFor({state: 'hidden'});
});

for (const name of ['config.json', 'themes.json']) {
    test(`Fehler beim Laden von ${name} ist im Viewer sichtbar`, async t => {
        const browser = await chromium.launch();
        t.after(() => browser.close());
        const page = await browser.newPage();
        await page.route(`**/${name}*`, route => route.fulfill({status: 502, body: 'SO!GIS ist vorübergehend nicht verfügbar.'}));
        await page.goto(entry({}));
        const status = page.getByRole('status').filter({hasText: 'Die Kartenkonfiguration von SO!GIS konnte nicht geladen werden.'});
        await status.waitFor();
        assert.equal(await page.locator('.ol-layer canvas').count(), 0);
    });
}

test('Externer WMS behält einen gegebenen Query-Parameter unverändert', async t => {
    const browser = await chromium.launch();
    t.after(() => browser.close());
    const page = await browser.newPage();
    const layer = external.replace('#', '?language=de#');
    const image = page.waitForResponse(response => response.ok() && response.url().startsWith('https://wfs.geodienste.ch/') &&
        response.headers()['content-type']?.startsWith('image/'));
    await page.goto(entry({l: layer, c: '2600779,1215505', s: '22694'}));
    const params = new URL((await image).url()).searchParams;
    assert.equal(params.get('language'), 'de');
    assert.equal(params.has('LANGUAGE'), false);
    assert.equal(params.get('LAYERS'), 'Hoheitsgrenzen');
    await page.waitForFunction(layer => new URL(location.href).searchParams.get('l') === layer, layer);
});
