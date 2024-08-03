package ch.so.agi.wgc.components.map;


import com.google.gwt.user.client.Window;

import static elemental2.dom.DomGlobal.console;

import java.util.List;

import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.WmsLayer;
import ch.so.agi.wgc.state.StateManager;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLDivElement;
import ol.Coordinate;
import ol.Extent;
import ol.MapBrowserEvent;
import ol.MapEvent;
import ol.View;

public class MapComponent {

    private ConfigManager configManager;
    
    private MapManager mapManager;
    
    private ViewManager viewManager;
    
    private StateManager stateManager;
    
    private WmtsManager wmtsManager;

    private WmsManager wmsManager;

    private ol.Map olMap;
    
    private HTMLDivElement mapTarget;

    public MapComponent() {
        configManager = ConfigManager.getInstance();
        mapManager = MapManager.getInstance();
        stateManager = StateManager.getInstance();
                
        olMap = mapManager.getMap();
        olMap.setTarget("ol-map");
        viewManager = new ViewManager(olMap);
        View view = viewManager.getView();
        olMap.setView(view);
        wmtsManager = new WmtsManager(olMap);
        wmsManager = new WmsManager(olMap);
        
        olMap.addClickListener(new ol.event.EventListener<MapBrowserEvent>() {
            @Override
            public void onEvent(MapBrowserEvent event) {
                stateManager.setState(StateManager.PARAM_CLICKED_COORD, event.getCoordinate());
            }       
        });

        
        olMap.addMoveEndListener(new ol.event.EventListener<MapEvent>() {
            @Override
            public void onEvent(MapEvent event) {
                String newUrl = Window.Location.getProtocol() + "//" + Window.Location.getHost() + Window.Location.getPath();
                newUrl += "?bl=" + (String) stateManager.getState(StateManager.PARAM_ACTIVE_BASEMAP);

                List<WmsLayer> wmsLayers = (List<WmsLayer>) stateManager.getState(StateManager.PARAM_ACTIVE_FOREGROUND_LAYERS);
                String layerNames = "";
                for (int i=0; i<wmsLayers.size(); i++) {
                    WmsLayer wmsLayer = wmsLayers.get(i);
                    String layerName = wmsLayer.getName();
                    String baseUrl = wmsLayer.getBaseUrl();
                    int transparency = wmsLayer.getTransparency();
                    boolean isVisible = wmsLayer.isVisible();
                    boolean isExternal = wmsLayer.isExternal();
                    
                    if (isExternal) {
                        layerName = "wms:" + baseUrl + "%23" + layerName;
                    }
                    
                    if (transparency > 0) {
                        layerName += "%5B" + String.valueOf(transparency) + "%5D";
                    }
                    
                    if (!isVisible) {
                        layerName += "%21";
                    }
                    
                    if (i != wmsLayers.size() - 1) {
                        layerName += ",";
                    }
                    
                    layerNames += layerName;
                }
                newUrl += "&l=" + layerNames;
                
                View view = olMap.getView();                
                Extent extent = view.calculateExtent(olMap.getSize());
                double easting = extent.getLowerLeftX() + (extent.getUpperRightX() - extent.getLowerLeftX()) / 2;
                double northing = extent.getLowerLeftY() + (extent.getUpperRightY() - extent.getLowerLeftY()) / 2;

                newUrl += "&c=" + String.valueOf(easting) + "," + String.valueOf(northing);
//                newUrl += "&z=" + String.valueOf(Double.valueOf(view.getZoom()).intValue());
//                newUrl += "&z=" + String.valueOf(view.getZoom());
                
                int scale = (int) (view.getResolution() * (378.0 / 0.1)); // TODO Validate calculation. Habe ich aus einer Liste von mir... Original war 357.14, das passt weniger gut zum Web GIS Client.
                newUrl += "&s=" + String.valueOf(scale);

                // Dann wird es nochmals encoded und funktioniert nicht mehr.
//                String newUrlEncoded = URL.encode(newUrl);
                
                stateManager.setState(StateManager.PARAM_BROWSER_URL, newUrl);
            }    
        });
        
        stateManager.subscribe(StateManager.PARAM_ACTIVE_BASEMAP, (oldBasemap, newBasemap) -> onAddBasemap((String)newBasemap));
        stateManager.subscribe(StateManager.PARAM_ACTIVE_FOREGROUND_LAYERS, (oldForegroundLayers, newForegroundLayers) -> onForegroundLayers((List<WmsLayer>)newForegroundLayers));
        stateManager.subscribe(StateManager.PARAM_MAP_CENTER, (oldMapCenter, newMapCenter) -> onChangeMapCenter((Coordinate) newMapCenter));
        // Entweder Zoomlevel oder Scale. Damit man den Search-String für WGC übernehmen kann, wird neu auch hier
        // die Scale verwendet.
        //stateManager.subscribe(StateManager.PARAM_MAP_ZOOM_LEVEL, (oldMapZoomLevel, newMapZoomLevel) -> onChangeZoomLevel((int) newMapZoomLevel));
        stateManager.subscribe(StateManager.PARAM_MAP_SCALE, (oldMapScale, newMapScale) -> onChangeScale((int) newMapScale));
    }
        
    private void onAddBasemap(String newBasemap) {
        wmtsManager.addBasemapLayer(newBasemap);        
    }
    
    private void onForegroundLayers(List<WmsLayer> newForegroundLayers) {
        wmsManager.addForegroundLayers(newForegroundLayers);
    }
    
    private void onChangeMapCenter(Coordinate center) {
        viewManager.setCenter(center);
    }
    
    private void onChangeZoomLevel(int zoomLevel) {
        viewManager.setZoomLevel(zoomLevel);
    }
    
    private void onChangeScale(int scale) {
        viewManager.setScale(scale);
    }
}
