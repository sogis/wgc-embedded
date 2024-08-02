package ch.so.agi.wgc.components.map;

import org.jboss.elemento.IsElement;

import com.google.gwt.user.client.Window;

import static elemental2.dom.DomGlobal.console;

import java.util.ArrayList;
import java.util.List;

import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.WmsLayer;
import ch.so.agi.wgc.state.StateManager;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import ol.Coordinate;
import ol.Extent;
import ol.MapEvent;
import ol.View;

public class MapComponent implements IsElement<HTMLElement> {

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
        
        olMap.addMoveEndListener(new ol.event.EventListener<MapEvent>() {

            @Override
            public void onEvent(MapEvent event) {
                console.log("zoom / pan");
                
                
                // hier die url basteln und dann als State verwalten 
                // update der Url in separater BrowserUrl Componente
                
                String newUrl = Window.Location.getProtocol() + "//" + Window.Location.getHost() + Window.Location.getPath();
                newUrl += "?bl=" + (String) stateManager.getState(StateManager.PARAM_ACTIVE_BASEMAP);
                console.log("newUrl1 : " + newUrl);
                
                View view = olMap.getView();                
                Extent extent = view.calculateExtent(olMap.getSize());
                double easting = extent.getLowerLeftX() + (extent.getUpperRightX() - extent.getLowerLeftX()) / 2;
                double northing = extent.getLowerLeftY() + (extent.getUpperRightY() - extent.getLowerLeftY()) / 2;


                
            }
            
        });
        

        stateManager.subscribe(StateManager.PARAM_ACTIVE_BASEMAP, (oldBasemap, newBasemap) -> onAddBasemap((String)newBasemap));
        stateManager.subscribe(StateManager.PARAM_ACTIVE_FOREGROUND_LAYERS, (oldForegroundLayers, newForegroundLayers) -> onForegroundLayers((List<WmsLayer>)newForegroundLayers));
        stateManager.subscribe(StateManager.PARAM_MAP_CENTER, (oldMapCenter, newMapCenter) -> onChangeMapCenter((Coordinate) newMapCenter));
        stateManager.subscribe(StateManager.PARAM_MAP_ZOOM_LEVEL, (oldMapZoomLevel, newMapZoomLevel) -> onChangeZoomLevel((int) newMapZoomLevel));
    }
    
    @Override
    public HTMLElement element() {
        return mapTarget;
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
    
    
}
