package ch.so.agi.wgc.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.user.client.Window;

import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.WmsLayer;
import ch.so.agi.wgc.state.StateManager;
import ol.Coordinate;

import static elemental2.dom.DomGlobal.console;

public class BrowserUrlParser {
    private static StateManager stateManager;

    public BrowserUrlParser() {
        stateManager = StateManager.getInstance();
    }
    
    public void parse() {
        String bgLayer = null;
        if (Window.Location.getParameter("bl") != null) {
            bgLayer = Window.Location.getParameter("bl").toString();
            stateManager.setState(StateManager.PARAM_ACTIVE_BASEMAP, bgLayer);
        }
        
        List<WmsLayer> wmsLayerList = new ArrayList<WmsLayer>();
        if (Window.Location.getParameter("l") != null) {
            String layersParam = Window.Location.getParameter("l");            
            if (layersParam != null) {
                List<String> layerParamList = Arrays.asList(layersParam.split(",", -1));        
                for (String layerParam : layerParamList) {                                       
                    // layerParam is not visible
                    boolean isVisible = true;
                    if (layerParam.endsWith("!")) {
                        isVisible = false;
                    }
                    
                    // get the transparency of layer
                    int transparency = 0;
                    RegExp regExp = RegExp.compile("\\[(\\d+)\\]");
                    MatchResult matcher = regExp.exec(layerParam);
                    if (matcher != null) {
                        transparency = Integer.valueOf(matcher.getGroup(1));
                    } 
                    
                    // get layer name and wms base url
                    String layerName = layerParam;
                    String baseUrlWms = null;
                    boolean isExternal = false;
                    if (!layerParam.startsWith("wms:")) {
                        baseUrlWms = ConfigManager.getInstance().getConfig().baseUrlWms;
                    }

                    if (layerParam.startsWith("wms:")) {                        
                        isExternal = true;
                        layerParam = layerParam.substring(4);                            
                        String layerParams[] = layerParam.split("#");
                        baseUrlWms = layerParams[0];
                        layerName = layerParams[1];    
                    }
                    
                    regExp = RegExp.compile("^[^\\[\\!]*");
                    matcher = regExp.exec(layerName);
                    if (matcher != null) {
                        String result = matcher.getGroup(0);
                        layerName = result;
                    } 

                    WmsLayer wmsLayer = new WmsLayer(layerName, baseUrlWms, isVisible, transparency, isExternal);
                    wmsLayerList.add(wmsLayer);
                }
                stateManager.setState(StateManager.PARAM_ACTIVE_FOREGROUND_LAYERS, wmsLayerList);
            }
        }
        
        double easting;
        double northing;
        if (Window.Location.getParameter("c") != null) {
            String center[] = Window.Location.getParameter("c").split(",");
            easting =  Double.valueOf(center[0]);
            northing = Double.valueOf(center[1]);
        } else {
            easting = 2616500;
            northing = 1237000;
        }
        stateManager.setState(StateManager.PARAM_MAP_CENTER, new Coordinate(easting, northing));

        if (Window.Location.getParameter("z") != null) {
            String zoomLevel = Window.Location.getParameter("z");
            stateManager.setState(StateManager.PARAM_MAP_ZOOM_LEVEL, Double.valueOf(zoomLevel).intValue());
        }
        
        if (Window.Location.getParameter("s") != null) {
            String scale = Window.Location.getParameter("s");
            stateManager.setState(StateManager.PARAM_MAP_SCALE, Double.valueOf(scale).intValue());
        }
    }
}
