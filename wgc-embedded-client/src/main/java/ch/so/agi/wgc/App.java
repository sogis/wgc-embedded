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

import ch.so.agi.wgc.tools.BrowserUrlParser;
import ch.so.agi.wgc.tools.BrowserUrlUpdater;
import ch.so.agi.wgc.components.featureinfo.FeatureInfoComponent;
import ch.so.agi.wgc.components.jumplink.JumpLinkComponent;
import ch.so.agi.wgc.components.map.MapComponent;
import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.WmsLayer;
import ch.so.agi.wgc.state.StateManager;
import elemental2.core.Global;
import elemental2.dom.DomGlobal;
import elemental2.dom.URL;
import elemental2.promise.Promise;
import ol.Coordinate;

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
        if (pathname.contains("index.html")) {
            pathname = pathname.replace("/index.html", "");
        }
        
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
        JumpLinkComponent wgcLinkComponent = new JumpLinkComponent();
        FeatureInfoComponent featureInfoComponent = new FeatureInfoComponent();
        
        // Updates the url when map is zoomed or panned.
        BrowserUrlUpdater browserUrlUpdater = new BrowserUrlUpdater();
        
        // Parses the url and sets various states.
        BrowserUrlParser browserUrlParser = new BrowserUrlParser();
        browserUrlParser.parse();
    }    
}