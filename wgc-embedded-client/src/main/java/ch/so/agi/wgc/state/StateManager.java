package ch.so.agi.wgc.state;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class StateManager {
    private static StateManager INSTANCE;
        
    private Map<String, Object> stateMap = new HashMap<>();
    private Map<String, BiConsumer<Object, Object>> subscribers = new HashMap<>();

    public static StateManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new StateManager();
        }
        
        return INSTANCE;
    }

    public void setState(String key, Object value) {
        Object oldValue = stateMap.put(key, value);
        notifySubscribers(key, oldValue, value);
    }

    public Object getState(String key) {
        return stateMap.get(key);
    }

    public void subscribe(String key, BiConsumer<Object, Object> subscriber) {
        subscribers.put(key, subscriber);
    }

    private void notifySubscribers(String key, Object oldValue, Object newValue) {
        BiConsumer<Object, Object> subscriber = subscribers.get(key);
        if (subscriber != null) {
            subscriber.accept(oldValue, newValue);
        }
    }
    
    public static final String PARAM_APP_BASE_URL = "state.param.app.base_url";
    public static final String PARAM_ACTIVE_BASEMAP = "activBasemap";
    public static final String PARAM_ACTIVE_LAYERS = "activeLayers";
}
