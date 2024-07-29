package ch.so.agi.wgc;

import static elemental2.dom.DomGlobal.console;
import static org.jboss.elemento.Elements.*;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;

import ch.so.agi.wgc.Config.BasemapConfig;
import elemental2.core.Global;
import elemental2.dom.DomGlobal;
import elemental2.dom.URL;

public class App implements EntryPoint {

    private static final String API_ENDPOINT_CONFIG = "/config";

    private String host; 
    private String protocol;
    private String pathname;

    public void onModuleLoad() {
        URL url = new URL(DomGlobal.location.href);
        host = url.host;
        protocol = url.protocol;
        pathname = url.pathname.length()==1?"":url.pathname;

        String requestUrl = protocol + "//" + host + pathname + API_ENDPOINT_CONFIG;

        DomGlobal.fetch(requestUrl).then(response -> {
            if (!response.ok) {
                DomGlobal.window.alert(response.statusText + ": " + response.body);
                return null;
            }
            return response.text();
        }).then(json -> {            
            console.log(json);
            
            Config myConfig = (Config) Global.JSON.parse(json);
//            console.log(myConfig.basemaps.get(0));
            
            console.log(myConfig.basemaps[0].url);
            console.log(myConfig.basemaps[1].url);
            
//            JsPropertyMap<Object> responseMap = Js.asPropertyMap(Global.JSON.parse(json));
//            JsPropertyMap<Object> profilesMap = Js.asPropertyMap(responseMap.get("profiles"));
//            profiles = new HashMap<>();
//            profilesMap.forEach(new JsForEachCallbackFn() {
//                @Override
//                public void onKey(String key) {
//                    String value = String.valueOf(profilesMap.get(key));
//                    profiles.put(key, value);
//                }                
//            });
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

//        body().add(TextBox.create().setLabel("User name")
//                                .setPlaceholder("Username").element());        
    }
}