package ch.so.agi.wgc.components.featureinfo;

import elemental2.dom.DomGlobal;
import elemental2.dom.HTMLElement;
import elemental2.dom.Headers;
import elemental2.dom.RequestInit;
import ol.Coordinate;
import ch.so.agi.wgc.components.map.MapManager;
import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.WmsLayer;
import ch.so.agi.wgc.state.StateManager;

import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.Text;
import com.google.gwt.xml.client.XMLParser;

import static elemental2.dom.DomGlobal.console;

public class FeatureInfoComponent {
    private HTMLElement root;    

    private StateManager stateManager;
    private MapManager mapManager;
    private ConfigManager configManager;
    private Config config;

    public FeatureInfoComponent() {
        stateManager = StateManager.getInstance();
        stateManager.subscribe(StateManager.PARAM_CLICKED_COORD, (oldClickedCoordinate, newClickedCoordinate) -> onClickMap((Coordinate)newClickedCoordinate));

        mapManager = MapManager.getInstance();
        
        configManager = ConfigManager.getInstance();
        config = configManager.getConfig();
        
        
        //body().add(root);
    }
    
    
    private void onClickMap(Coordinate newClickedCoordinate) {
        console.log(newClickedCoordinate);
        
        double resolution = mapManager.getMap().getView().getResolution();
        
        // 50/51/101-Ansatz ist anscheinend bei OpenLayers normal.
        // -> siehe baseUrlFeatureInfo resp. ein Original-Request
        // im Web GIS Client.
        double minX = newClickedCoordinate.getX() - 50 * resolution;
        double maxX = newClickedCoordinate.getX() + 51 * resolution;
        double minY = newClickedCoordinate.getY() - 50 * resolution;
        double maxY = newClickedCoordinate.getY() + 51 * resolution;

        String baseUrlFeatureInfo = config.baseUrlFeatureInfo;
        console.log(baseUrlFeatureInfo);
        
        List<WmsLayer> foregroundLayers = (List<WmsLayer>) stateManager.getState(StateManager.PARAM_ACTIVE_FOREGROUND_LAYERS);
        List<String> internalLayers = new ArrayList<>();
        for (WmsLayer wmsLayer : foregroundLayers) {
            if (!wmsLayer.isExternal() && wmsLayer.isVisible()) {
                internalLayers.add(wmsLayer.getName());
            }
        }
        
        String layers = String.join(",", internalLayers);
        String urlFeatureInfo = baseUrlFeatureInfo + "&layers=" + layers;
        urlFeatureInfo += "&query_layers=" + layers;
        urlFeatureInfo += "&bbox=" + minX + "," + minY + "," + maxX + "," + maxY;
        console.log(urlFeatureInfo);
        
        RequestInit requestInit = RequestInit.create();
        Headers headers = new Headers();
        headers.append("Content-Type", "application/x-www-form-urlencoded"); 
        requestInit.setHeaders(headers);

        DomGlobal.fetch(urlFeatureInfo)
        .then(response -> {
            if (!response.ok) {
                return null;
            }
            return response.text();
        })
        .then(xml -> {
            Document messageDom = XMLParser.parse(xml);
            console.log(messageDom);
            
            
            return null;
        })
        .catch_(error -> {
            console.log(error);
            DomGlobal.window.alert(error);
            return null;
        });
    }
}
