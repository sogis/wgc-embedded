package ch.so.agi.wgc.components.map.browserurl;

import ch.so.agi.wgc.components.map.MapManager;

public class BrowserUrlComponent {    
    private ol.Map map;

    public BrowserUrlComponent() {
        map = MapManager.getInstance().getMap();
        
        
        
    }
}
