package ch.so.agi.wgc;

import ol.MapOptions;
import ol.OLFactory;
import ol.interaction.DefaultInteractionsOptions;
import ol.interaction.Interaction;
import ol.interaction.MouseWheelZoom;
import ol.interaction.MouseWheelZoomOptions;

public class MapManager {
    private static MapManager INSTANCE;
    
    private static ol.Map olMap;

    public static MapManager getInstance() {
        if(INSTANCE == null) {
            INSTANCE = new MapManager();
            
            MapOptions mapOptions = OLFactory.createOptions();
            
            MouseWheelZoomOptions mouseWheelZoomOptions = new MouseWheelZoomOptions();
            mouseWheelZoomOptions.setConstrainResolution(true);
            
            
            DefaultInteractionsOptions interactionOptions = new ol.interaction.DefaultInteractionsOptions();
            interactionOptions.setPinchRotate(false);
            
            MouseWheelZoom mwz = new MouseWheelZoom();
           
//            mwz.set("constrainResolution", true);
//            interactionOptions.setConstrainResolution(true);
//            interactionOptions.setDoubleClickZoom(false);
//            mapOptions.setInteractions(Interaction.defaults(interactionOptions));
            
//            console.log(mwz.getProperties());
            
            
            // siehe https://github.com/TDesjardins/gwt-ol/commit/2fa5b8cd39f221dba34f9e361ef96559985037c6 
            // Konstruktor mit options
            
            olMap = new ol.Map(mapOptions);
            

        }
        
        return INSTANCE;
    }
    
    public ol.Map getMap() {
        return olMap;
    }

}
