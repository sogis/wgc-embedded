package ch.so.agi.wgc.config;

import static elemental2.dom.DomGlobal.console;
import static elemental2.dom.DomGlobal.fetch;

import ch.so.agi.wgc.state.StateManager;
import elemental2.core.Global;
import elemental2.dom.DomGlobal;
import elemental2.promise.Promise;
import jsinterop.base.Js;

public class ConfigManager {
    private static ConfigManager INSTANCE;
    
    public static final String API_ENDPOINT_CONFIG = "/config";

    private Config config = null;
    
    private Promise<Config> loadingPromise = null; 

    public static ConfigManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new ConfigManager();
        }
        return INSTANCE;
    }
    
    public Config getConfig() {
        return config;
    }
    
    
    public Promise<Config> loadConfig() {
        StateManager stateManager = StateManager.getInstance();

        String appBaseUrl = (String) stateManager.getState(StateManager.PARAM_APP_BASE_URL);
        String request = appBaseUrl + API_ENDPOINT_CONFIG;

        if (loadingPromise != null) {
            // There's already a promise for loading the configuration
            // => return it instead of starting another request
            console.log("There's already a promise for loading the configuration.");
            return loadingPromise;
        }

        if (config != null) {
            // Config was already loaded.
            // => stop here
            console.log("Config was already loaded.");
            return Promise.resolve(config);
        }

        console.log("Loading Application Configuration...");
        loadingPromise = fetch(request)
                .then(response -> {
                    if (!response.ok) {
                        DomGlobal.window.alert(response.statusText + ": " + response.body);
                        return null;
                    }
                    return response.text();
                }).then(textConfig -> {
                    config = Js.cast(Global.JSON.parse(textConfig));
                    
//                    String id = config.basemaps[0].url;
//                    console.log(id);
//                    double[] resolutions = config.basemaps[0].resolutions;
//                    console.log(resolutions);
                                       
                    return null;
                });
        return loadingPromise;
    }
}
