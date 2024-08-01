package ch.so.agi.wgc;

import static elemental2.dom.DomGlobal.console;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;

import ch.so.agi.wgc.components.MapComponent;
import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.state.StateManager;
import elemental2.core.Global;
import elemental2.dom.DomGlobal;
import elemental2.dom.URL;
import elemental2.promise.Promise;

public class App implements EntryPoint {

    private String host; 
    private String protocol;
    private String pathname;

    private StateManager stateManager;
    
    public void onModuleLoad() {
        URL url = new URL(DomGlobal.location.href);
        host = url.host;
        protocol = url.protocol;
        pathname = url.pathname.length()==1?"":url.pathname;

        stateManager = StateManager.getInstance();
        stateManager.setState(StateManager.PARAM_APP_BASE_URL, protocol + "//" + host + pathname);
        
        ConfigManager configManager = ConfigManager.getInstance();
        Promise<Config> configPromise = configManager.loadConfig();
        
        Promise.all(configPromise).then(resolved -> {
            init();
            return null;
        }).catch_(error -> {
            console.log(error);
            DomGlobal.window.alert(error.toString());
            return null;
        });        
    }
    
    private void init() {

        console.log("Hallo Welt.");
        console.log("fubar");

        
        MapComponent mapComponent = new MapComponent();
        
        String bgLayer = null;
        if (Window.Location.getParameter("bgLayer") != null) {
            bgLayer = Window.Location.getParameter("bgLayer").toString();
        }
        stateManager.setState(StateManager.PARAM_ACTIVE_BASEMAP, bgLayer);
        
        List<String> layerList = new ArrayList<String>();
        if (Window.Location.getParameter("layers") != null) {
            String layers = Window.Location.getParameter("layers").toString();
            layerList = Arrays.asList(layers.split(",", -1));
        }
        stateManager.setState(StateManager.PARAM_ACTIVE_LAYERS, layerList);
        

        // TODO: api eher wie bei uns [xx] und !.
        // Layermodell mit den notwendigen Parametern (name, opacity, visibilty)
        // Liste von layermodell in den State.
        
//        body().add(TextBox.create().setLabel("User name")
//                                .setPlaceholder("Username").element());        
    }
}