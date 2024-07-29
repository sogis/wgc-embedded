package ch.so.agi.wgc.state;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class StateManager {
    private static StateManager INSTANCE;
        
    private Map<String, String> stateMap = new HashMap<>();
    private Map<String, BiConsumer<String, String>> subscribers = new HashMap<>();

    public static StateManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new StateManager();
        }
        
        return INSTANCE;
    }

    public void setState(String key, String value) {
        String oldValue = stateMap.put(key, value);
        notifySubscribers(key, oldValue, value);
    }

    public String getState(String key) {
        return stateMap.get(key);
    }

    public void subscribe(String key, BiConsumer<String, String> subscriber) {
        subscribers.put(key, subscriber);
    }

    private void notifySubscribers(String key, String oldValue, String newValue) {
        BiConsumer<String, String> subscriber = subscribers.get(key);
        if (subscriber != null) {
            subscriber.accept(oldValue, newValue);
        }
    }
    
    public static final String STATE_PARAM_APP_BASE_URL = "state.param.app.base_url";
}
