package ch.so.agi.wgc.components;

import org.jboss.elemento.IsElement;

import static elemental2.dom.DomGlobal.console;

import java.util.ArrayList;
import java.util.List;

import ch.so.agi.wgc.MapManager;
import ch.so.agi.wgc.ViewManager;
import ch.so.agi.wgc.WmtsManager;
import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.state.StateManager;
import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import ol.View;

public class MapComponent implements IsElement<HTMLElement> {

    private ConfigManager configManager;
    
    private MapManager mapManager;
    
    private ViewManager viewManager;
    
    private StateManager stateManager;
    
    private WmtsManager wmtsManager;
    
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

        stateManager.subscribe(StateManager.PARAM_ACTIVE_BASEMAP, (oldBasemap, newBasemap) -> onAddBasemap((String)newBasemap));
        
        stateManager.subscribe(StateManager.PARAM_ACTIVE_LAYERS, (oldActiveLayers, newActiveLayers) -> onAddActiveLayers((List<String>)newActiveLayers));
    }
    
    @Override
    public HTMLElement element() {
        return mapTarget;
    }
    
    private void onAddBasemap(String newBasemap) {
        wmtsManager.addBasemapLayer(newBasemap);        
    }
    
    private void onAddActiveLayers(List<String> newActiveLayers) {
        console.log(newActiveLayers);
    }
}
