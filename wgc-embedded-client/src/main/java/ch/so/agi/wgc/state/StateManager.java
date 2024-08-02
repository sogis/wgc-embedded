package ch.so.agi.wgc.state;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import static elemental2.dom.DomGlobal.console;

public class StateManager {
    private static StateManager INSTANCE;
        
    private Map<String, Object> stateMap = new HashMap<>();
//    private Map<String, BiConsumer<Object, Object>> subscribers = new HashMap<>();
    private Map<String, List<BiConsumer<Object, Object>>> subscribers = new HashMap<>();

    public static StateManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new StateManager();
        }
        
        return INSTANCE;
    }

    public void setState(String key, Object value) {
        Object oldValue = stateMap.put(key, value);
//        notifySubscribers(key, oldValue, value);
        notifySubscribers(key, oldValue, value);
    }

    public Object getState(String key) {
        return stateMap.get(key);
    }

//    public void subscribe(String key, BiConsumer<Object, Object> subscriber) {
//        subscribers.put(key, subscriber);
//    }
    
    public void subscribe(String key, BiConsumer<Object, Object> subscriber) {        
        if (subscribers.get(key) != null) {
//            console.log("Add following subscriber");
            subscribers.get(key).add(subscriber);            
        } else {
//            console.log("Add first subscriber");
            List<BiConsumer<Object, Object>> biConsumerList = new ArrayList();
            biConsumerList.add(subscriber);
            subscribers.put(key, biConsumerList);
        }
    }
    
//    private void notifySubscribers(String key, Object oldValue, Object newValue) {
//        BiConsumer<Object, Object> subscriber = subscribers.get(key);
//        if (subscriber != null) {
//            subscriber.accept(oldValue, newValue);
//        }
//    }

    private void notifySubscribers(String key, Object oldValue, Object newValue) {
        List<BiConsumer<Object, Object>> subscriberList = subscribers.get(key);
        if (subscriberList != null) {
            for (BiConsumer<Object, Object> subscriber : subscriberList) {
//                console.log("subscriber: " + subscriber);
                subscriber.accept(oldValue, newValue);
            }            
        }
    }

    public static final String PARAM_APP_BASE_URL = "state.param.app.base_url";
    public static final String PARAM_ACTIVE_BASEMAP = "activeBasemap";
    public static final String PARAM_ACTIVE_FOREGROUND_LAYERS = "activeForegroundLayers";
    public static final String PARAM_MAP_CENTER = "mapCenter";
    public static final String PARAM_MAP_ZOOM_LEVEL = "mapZoomLevel";
    public static final String PARAM_MAP_SCALE = "mapScale";
    public static final String PARAM_BROWSER_URL = "browserUrl";
}
