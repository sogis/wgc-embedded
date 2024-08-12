package ch.so.agi.wgc.components.jumplink;

import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.Config.BasemapLayer;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.state.StateManager;
import elemental2.dom.HTMLAnchorElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.MutationRecord;
import elemental2.dom.URL;
import elemental2.dom.URLSearchParams;

import static org.jboss.elemento.Elements.*;
import static elemental2.dom.DomGlobal.console;

public class JumpLinkComponent {
    private HTMLElement root;    
    private HTMLAnchorElement anchorElement;

    private ConfigManager configManager;
    private Config config;
    private StateManager stateManager;

    public JumpLinkComponent() {
        configManager = ConfigManager.getInstance();
        config = configManager.getConfig();
                
        stateManager = StateManager.getInstance();
        stateManager.subscribe(StateManager.PARAM_BROWSER_URL, (oldBrowserUrl, newBrowserUrl) -> onChangeBrowserUrl((String)newBrowserUrl));

        // Es scheint, als ob der Name "favicon" irgendwo geblockt wird.
        // Mit "fubar.png" funktioniert.
        // Hier jetzt mit base64 gelöst.
        String imageUrl = "fubar.png";  
        String href = (String) stateManager.getState(StateManager.PARAM_BROWSER_URL);
        anchorElement = a().css("wgcLink").attr("href", href).attr("target", "_blank").add(span().textContent("In geo.so.ch/map ansehen")).element();
        root = div().css("wgcLinkContainer")
                //.add(img().attr("src", imageUrl).attr("width", "16"))
                .add(img().attr("src", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAACXBIWXMAAAFNAAABTQG7wV/dAAAAGXRFWHRTb2Z0d2FyZQB3d3cuaW5rc2NhcGUub3Jnm+48GgAAAgxJREFUWIXtlU9rGkEYh3+zWyeJkqYGZUHU6bG9pgRBxIMnbz0U+gV6DPTcQA81h5rPoKfeihRaFEygeLWI5OJFKAFBhFAsG/wzrFozby/boIkm0rrQw/5gD/O+wzzPDDsMIyIAAGPsZcTn+7AB6HAwI6WoY1lxIjoDgAczva1nuu552+87KvCac+oA/M9YcxK2SlwBV8AVcAVcAVdg9jUEj8flk6Ojh04C+cHBFPX6YgG2s6N8+/tO8qFtb6u5saO0FfJfCYwnk4laOnNNGY/HBEAuEmi2221yWsA0zSsArUUCLdM0HRVQSsGyrCkRDW4JEFFfSnnZbDYdEyiXy+Cct2drcz9hbzD48rFQ+OWUwMnpKQ2Hw09zRSK6/gA8FUJ0pZS07nQ6HRJCXALYnWVqN2Sapml+fpfJWOve/ZvDwytiLEdE5tITsE9hNxgMtkqlklrX7nO5HEUikXMAnlu8mwVbYi8UCl1Uq9V/hheLRRJC/AAQXchaVLQlUoZhtPP5/PRv4dnjYyWEuPB6vXtLOcsatsTjQCBwlk6ne/V6fWVwpVKhZDIpo9FoFYD/TsZdTVuCeTyeV4ZhnCcSCfN9NqsajQZZlnUNlFJSrVajTCajYrFYLxwOf+ecv7hvbSICsyH3hjGmAUg98vufb21upgDsaJq2AYARkaXres8ajb7+7HYLRPRt1dvxG3gh0UeKP+U/AAAAAElFTkSuQmCC").attr("width", "16"))
                .add(span().add(anchorElement))
                .element();
        
        body().add(root);        
    }
        
    private void onChangeBrowserUrl(String newBrowserUrl) {
        String baseUrlWgc = config.baseUrlWgc;        
        String urlWgc = baseUrlWgc;
        
        //console.log("2: " + newBrowserUrl);
        
        // TODO
        // Vielleicht am falschen Ort. Jetzt wird es jedes Mal ausgeführt.
        // Das Mapping zur Id kann man gleich beim Auslesen der URL in App.java
        // machen.
        // Falls er den bgLayer nicht in der Config findet, leer lassen.
        URL url = new URL(newBrowserUrl);        
        if (url.searchParams.get("bl") != null) {
            String bgLayer = url.searchParams.get("bl");
            String layerId = null;
            for (BasemapLayer basemapLayer : config.basemaps[0].layers) {
                if (basemapLayer.title.equalsIgnoreCase(bgLayer)) {
                    layerId = basemapLayer.id;
                }
            }
            URLSearchParams params = url.searchParams;
            params.set("bl", layerId);
            urlWgc += "?" + params.toString(); 
        } else {
            urlWgc += url.search;
        }
        
        anchorElement.setAttribute("href", urlWgc);
    }
}
