package ch.so.agi.wgc.config;

import static elemental2.dom.DomGlobal.console;

import java.util.Arrays;

import ch.so.agi.wgc.Constants;
import ch.so.agi.wgc.state.StateManager;
import elemental2.dom.DomGlobal;
import elemental2.dom.XMLHttpRequest;

public class ConfigManager {
    private static ConfigManager INSTANCE;
    
    private Config config = null;
    
    public static ConfigManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ConfigManager();
        }
        
        return INSTANCE;
    }
    
    public Config getConfig() {
        return config;
    }
    
    public void loadConfig() {
        StateManager stateManager = StateManager.getInstance();
        console.log(stateManager.getState(StateManager.PARAM_APP_BASE_URL));
        
        String appBaseUrl = stateManager.getState(StateManager.PARAM_APP_BASE_URL);
        String request = appBaseUrl + Constants.API_ENDPOINT_CONFIG;
        console.log("load config");
        
        XMLHttpRequest httpRequest = new XMLHttpRequest();
        httpRequest.open("GET", request, false);
        httpRequest.onload = event -> {
            if (Arrays.asList(200, 201, 204).contains(httpRequest.status)) {
                String responseText = httpRequest.responseText;
                console.log(responseText);
//                try {
//                    JsPropertyMap<Object> propertiesMap = Js.asPropertyMap(Global.JSON.parse(responseText));
//                    FILES_SERVER_URL = propertiesMap.getAsAny("filesServerUrl").asString();
//
//                } catch (Exception e) {
//                    DomGlobal.window.alert("Error loading settings!");
//                    DomGlobal.console.error("Error loading settings!", e);
//                }
            } else {
                DomGlobal.window.alert("Error loading config!" + httpRequest.status);
            }

        };

        httpRequest.addEventListener("error", event -> {
            DomGlobal.window
                    .alert("Error loading config! Error: " + httpRequest.status + " " + httpRequest.statusText);
        });

        httpRequest.send();

        
    }

}
