package ch.so.agi.wgc;

import static elemental2.dom.DomGlobal.console;
import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.user.client.Window;

import ch.so.agi.wgc.components.map.MapComponent;
import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.WmsLayer;
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
        MapComponent mapComponent = new MapComponent();
        
        String bgLayer = null;
        if (Window.Location.getParameter("bl") != null) {
            bgLayer = Window.Location.getParameter("bl").toString();
            stateManager.setState(StateManager.PARAM_ACTIVE_BASEMAP, bgLayer);
        }
        
        List<WmsLayer> wmsLayerList = new ArrayList<WmsLayer>();
        if (Window.Location.getParameter("l") != null) {
            String layersParam = Window.Location.getParameter("l");
            if (layersParam != null) {
                List<String> layerParamList = Arrays.asList(layersParam.split(",", -1));        
                
                for (String layerParam : layerParamList) {                   
                    // layerParam is not visible
                    boolean isVisible = true;
                    if (layerParam.endsWith("!")) {
                        isVisible = false;
                    }
                    
                    // get the transparency of layer
                    int transparency = 0;
                    RegExp regExp = RegExp.compile("\\[(\\d+)\\]");
                    MatchResult matcher = regExp.exec(layerParam);
                    if (matcher != null) {
                        transparency = Integer.valueOf(matcher.getGroup(1));
                    } 
                    
                    // get layer name
                    String layerName = layerParam;
                    regExp = RegExp.compile("^[^\\[\\!]*");
                    matcher = regExp.exec(layerParam);
                    if (matcher != null) {
                        String result = matcher.getGroup(0);
                        layerName = result;
                    } 
                    
                    String baseUrlWms = ConfigManager.getInstance().getConfig().baseUrlWms;
                    
                    WmsLayer wmsLayer = new WmsLayer(layerName, baseUrlWms, isVisible, transparency);
                    wmsLayerList.add(wmsLayer);
                }
                stateManager.setState(StateManager.PARAM_ACTIVE_LAYERS, wmsLayerList);
            }
        }
                
//        body().add(TextBox.create().setLabel("User name")
//                                .setPlaceholder("Username").element());        
    }
}