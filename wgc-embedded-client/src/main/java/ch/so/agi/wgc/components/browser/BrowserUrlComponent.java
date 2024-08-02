package ch.so.agi.wgc.components.browser;

import ch.so.agi.wgc.state.StateManager;

import static elemental2.dom.DomGlobal.console;

public class BrowserUrlComponent {
    private static StateManager stateManager;
    
    private ol.Map map;

    public BrowserUrlComponent() {
        stateManager = StateManager.getInstance();
        stateManager.subscribe(StateManager.PARAM_BROWSER_URL, (oldBrowserUrl, newBrowserUrl) -> onChangeBrowserUrl((String)newBrowserUrl));
    }
    
    private void onChangeBrowserUrl(String newBrowserUrl) {        
        this.updateURLWithoutReloading(newBrowserUrl);
    }
    
    private static native void updateURLWithoutReloading(String newUrl) /*-{
        $wnd.history.pushState(newUrl, "", newUrl);
    }-*/; 
}
