import axios from 'axios';
import React from 'react';
import {createRoot} from 'react-dom/client';
import StandardApp from 'qwc2-lts/components/StandardApp';
import Identify from 'qwc2-lts/plugins/Identify';
import MapPlugin from 'qwc2-lts/plugins/Map';
import locale from 'qwc2-lts/static/translations/de-CH.json';
import ConfigUtils from 'qwc2-lts/utils/ConfigUtils';
import {LayerRole, removeLayer} from 'qwc2-lts/actions/layers';
import {CHANGE_MAP_VIEW} from 'qwc2-lts/actions/map';
import LayerUtils from 'qwc2-lts/utils/LayerUtils';
import MapUtils from 'qwc2-lts/utils/MapUtils';
import {UrlParams} from 'qwc2-lts/utils/PermaLinkUtils';
import './icons/build/qwc2-icons.css';
import './viewer.css';

const mapKeys = ['bl', 'l', 'c', 's'];
const incoming = new URLSearchParams(location.search);
const initial = new URLSearchParams();
for (const key of mapKeys) {
    if (incoming.has(key)) initial.set(key, incoming.get(key));
}
const background = initial.get('bl')?.replace(/^ch\.so\.agi\./, '');
initial.set('bl', background || 'hintergrundkarte_sw');
const center = initial.get('c')?.split(',');
if (center?.length !== 2 || !center.every(value => value.trim() && Number.isFinite(Number(value)))) {
    initial.set('c', '2616500,1237000');
}
const scale = Number(initial.get('s'));
if (!Number.isFinite(scale) || scale <= 0) initial.set('s', '378000');
history.replaceState(history.state, '', '?' + initial);

const fullMap = document.getElementById('full-map');
function updateFullMap() {
    const current = new URLSearchParams(location.search);
    const target = new URL('https://geo.so.ch/map/');
    for (const key of mapKeys) {
        const value = current.get(key) ?? initial.get(key);
        if (value) target.searchParams.set(key, value);
    }
    // The QWC2 URL is written asynchronously after a move. Read the view at click time.
    const view = MapUtils.getHook(MapUtils.GET_MAP)?.getView();
    if (view?.getCenter() && view.getResolution()) {
        target.searchParams.set('c', view.getCenter().map(Math.round).join(','));
        target.searchParams.set('s', Math.round(view.getResolution() * MapUtils.dpi2dpm()));
    }
    fullMap.href = target.href;
}
updateFullMap();
for (const event of ['click', 'auxclick', 'contextmenu']) fullMap.addEventListener(event, updateFullMap);

axios.interceptors.response.use(undefined, error => {
    if (['config.json', 'themes.json'].includes(error.config?.url)) {
        const status = document.getElementById('loading');
        status.setAttribute('data-error', '');
        status.textContent = 'Die Kartenkonfiguration von SO!GIS konnte nicht geladen werden.';
    }
    return Promise.reject(error);
});

// The public SO!GIS weblayers endpoint maps URL names to QGIS layer definitions.
function themeLayerRestorer(missingLayers, theme, callback) {
    const service = ConfigUtils.getConfigProp('dataproductServiceUrl');
    axios.get(service + 'weblayers', {params: {filter: missingLayers.join(',')}}).then(({data}) => {
        const layerNames = Object.fromEntries(Object.entries(data).map(([name, sublayers]) =>
            [name, LayerUtils.getSublayerNames({sublayers})]));
        callback(Object.values(data).flat().filter(layer => Object.keys(layer).length), layerNames);
    }, () => callback([], null));
}

function externalLayerRestorer(externalLayers, themes, callback) {
    for (const [resource, layers] of Object.entries(externalLayers)) {
        const endpoint = resource.slice('wms:'.length);
        let service;
        try {
            service = new URL(endpoint);
        } catch {
            // Invalid URLs are discarded below along with unsupported layer types.
        }
        if (!resource.startsWith('wms:') || !service || !['http:', 'https:'].includes(service.protocol) ||
            service.username || service.password || endpoint.includes('&')) {
            for (const layer of layers) StandardApp.store.dispatch(removeLayer(layer.id));
            continue;
        }
        // Keep an endpoint query (e.g. a case-sensitive vendor parameter) verbatim.
        // QWC2 otherwise copies its keys in upper case into the WMS request.
        const query = service.search ? {
            sourceConfig: {url: endpoint},
            dimensionValues: Object.fromEntries([...service.searchParams.keys()].map(key => [key.toUpperCase(), undefined]))
        } : {};
        for (const layer of layers) {
            callback(layer.id, {
                ...query,
                ...layer,
                type: 'wms',
                url: endpoint,
                role: LayerRole.USERLAYER,
                version: '1.3.0',
                params: {LAYERS: layer.name},
                queryable: false
            });
        }
    }
}

createRoot(document.getElementById('container')).render(<StandardApp appConfig={{
    defaultLocaleData: locale,
    getDefaultLocale: () => 'de-CH',
    pluginsDef: {plugins: {MapPlugin: MapPlugin({}), IdentifyPlugin: Identify}, cfg: {}},
    themeLayerRestorer,
    externalLayerRestorer,
    themePreprocessor(themes) {
        const theme = themes.items.find(item => item.id === themes.defaultTheme);
        const requested = Number(initial.get('s'));
        const scales = theme.scales;
        // An exact step avoids QWC2's linear interpolation of OpenLayers' logarithmic zoom.
        if (requested < scales[0] && requested > scales.at(-1) && !scales.includes(requested)) {
            theme.scales = [...scales, requested].sort((left, right) => right - left);
        }
    },
    actionLogger(action) {
        if (action.type !== CHANGE_MAP_VIEW) return;
        const resolution = MapUtils.getHook(MapUtils.GET_MAP)?.getView().getResolution();
        // Keep QWC2's URL queue and correct just the fractional scale it writes.
        if (resolution) UrlParams.updateParams({s: Math.round(resolution * MapUtils.dpi2dpm())});
    }
}} />);
