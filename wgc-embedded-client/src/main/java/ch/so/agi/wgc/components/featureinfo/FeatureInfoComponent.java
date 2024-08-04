package ch.so.agi.wgc.components.featureinfo;

import elemental2.dom.DomGlobal;
import elemental2.dom.Event;
import elemental2.dom.EventListener;
import elemental2.dom.HTMLButtonElement;
import elemental2.dom.HTMLDivElement;
import elemental2.dom.HTMLElement;
import elemental2.dom.Headers;
import elemental2.dom.RequestInit;
import ol.Coordinate;
import ch.so.agi.wgc.components.map.MapManager;
import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.FeatureInfoResponse;
import ch.so.agi.wgc.models.WmsLayer;
import ch.so.agi.wgc.state.StateManager;

import static org.jboss.elemento.Elements.*;

import java.util.ArrayList;
import java.util.List;

import org.gwtproject.safehtml.shared.SafeHtmlUtils;
import org.jboss.elemento.HTMLContainerBuilder;

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
    }
    
    
    private void onClickMap(Coordinate newClickedCoordinate) {        
        double resolution = mapManager.getMap().getView().getResolution();
        
        // 50/51/101-Ansatz ist anscheinend bei OpenLayers normal.
        // -> siehe baseUrlFeatureInfo resp. ein Original-Request
        // im Web GIS Client.
        double minX = newClickedCoordinate.getX() - 50 * resolution;
        double maxX = newClickedCoordinate.getX() + 51 * resolution;
        double minY = newClickedCoordinate.getY() - 50 * resolution;
        double maxY = newClickedCoordinate.getY() + 51 * resolution;

        String baseUrlFeatureInfo = config.baseUrlFeatureInfo;
        
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
            List<FeatureInfoResponse> featureInfoResponses = new ArrayList<>();
            
            Document messageDom = XMLParser.parse(xml);            
            if (messageDom.getElementsByTagName("Feature").getLength() == 0) {
                console.log("Keine weiteren Informationen");
                //root.appendChild(div().css("popupNoContent").textContent("Keine weiteren Informationen").element());
            }

            for (int i=0; i<messageDom.getElementsByTagName("Layer").getLength(); i++) {
                Node layerNode = messageDom.getElementsByTagName("Layer").item(i);
                String layerName = ((com.google.gwt.xml.client.Element) layerNode).getAttribute("layername"); 
                String layerTitle = ((com.google.gwt.xml.client.Element) layerNode).getAttribute("name"); 

                if (layerNode.getChildNodes().getLength() == 0) {
                    continue;
                };

                NodeList htmlNodes = ((com.google.gwt.xml.client.Element) layerNode).getElementsByTagName("HtmlContent");
                for (int j=0; j<htmlNodes.getLength(); j++) {
                    Text htmlNode = (Text) htmlNodes.item(j).getFirstChild();
                    
                    String urlReport = null;
                    com.google.gwt.xml.client.Element layerElement = ((com.google.gwt.xml.client.Element) layerNode);
                    if (layerElement.getAttribute("featurereport") != null) {                                
                        double x = newClickedCoordinate.getX();
                        double y = newClickedCoordinate.getY();
                        
                        com.google.gwt.xml.client.Element featureNode = ((com.google.gwt.xml.client.Element) htmlNodes.item(j).getParentNode());
                        String featureId = featureNode.getAttribute("id");

                        urlReport = config.baseUrlReport + layerElement.getAttribute("featurereport") + "?feature=" + featureId +
                                "&x=" + String.valueOf(x) + "&y=" + String.valueOf(y) + "&crs=EPSG%3A2056";     
                    }
                    
                    FeatureInfoResponse featureInfoResponse = new FeatureInfoResponse(layerName, layerTitle, htmlNode.getData(), urlReport);
                    featureInfoResponses.add(featureInfoResponse);
                }
            }
            
            render(featureInfoResponses);
            
            return null;
        })
        .catch_(error -> {
            console.log(error);
            DomGlobal.window.alert(error);
            return null;
        });
    }
    
    private void render(List<FeatureInfoResponse> featureInfoResponses) {
        if (root != null) {
            root.remove();
        }
        
        HTMLContainerBuilder<HTMLDivElement> featureInfoResponseElementBuilder = div().id("feature-info-response");
        
        HTMLElement iconSpan = span().id("feature-info-response-close-icon").textContent("×").element();
        iconSpan.addEventListener("click", new EventListener() {
            @Override
            public void handleEvent(Event evt) {
                root.remove();
            }
        });
        
        featureInfoResponseElementBuilder.add(
                div().id("feature-info-response-header")
                .add(span().id("feature-info-response-header-text-span").textContent("Objektinformation"))
                .add(span().id("feature-info-response-header-button-span").add(iconSpan))
                ); 

        root = (HTMLDivElement) featureInfoResponseElementBuilder.element();
        root.hidden = true;
        body().add(root);

        
        for (FeatureInfoResponse response : featureInfoResponses) {  
            root.appendChild(div().css("feature-info-response-layer-header").textContent(response.getLayerTitle()).element());     

            HTMLContainerBuilder<HTMLDivElement> featureInfoResponseContentElementBuilder = div().css("feature-info-response-content"); // popupContent
            HTMLDivElement featureInfoHtml = div().innerHtml(SafeHtmlUtils.fromTrustedString(response.getHtmlContent())).element();
            featureInfoResponseContentElementBuilder.add(featureInfoHtml);

            if (response.getReportUrl() != null) {                                
                HTMLButtonElement htmlButtonElement = button().css("feature-info-response-report-button").textContent("Objektblatt").element();
                featureInfoResponseContentElementBuilder.add(htmlButtonElement);
                
                htmlButtonElement.addEventListener("click", new EventListener() {
                    @Override
                    public void handleEvent(Event evt) {
                        Window.open(response.getReportUrl(), "_blank", null);
                    }
                    
                });
                
                
            }
            
            
            root.appendChild(featureInfoResponseContentElementBuilder.element());

        }
        
        root.hidden = false;
    }
}
