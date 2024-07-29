package ch.so.agi.wgc.config;

import static elemental2.dom.DomGlobal.console;

import ch.so.agi.wgc.state.StateManager;

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
        console.log(stateManager.getState(StateManager.STATE_PARAM_APP_BASE_URL));
        
        console.log("load config");
    }

}
