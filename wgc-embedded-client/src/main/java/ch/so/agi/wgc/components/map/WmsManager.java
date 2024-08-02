package ch.so.agi.wgc.components.map;

import java.util.List;

import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ch.so.agi.wgc.models.WmsLayer;
import ol.OLFactory;
import ol.layer.LayerOptions;
import ol.source.ImageWms;
import ol.source.ImageWmsOptions;
import ol.source.ImageWmsParams;

import static elemental2.dom.DomGlobal.console;

public class WmsManager {
    private static ConfigManager configManager;
    
    private Config config;
    
    private ol.Map map;

    public WmsManager(ol.Map map) {
        this.map = map;
        configManager = ConfigManager.getInstance();
        config = configManager.getConfig();
    }
    
    public void addForegroundLayers(List<WmsLayer> newForegroundLayers) {
        for (WmsLayer forgroundLayer : newForegroundLayers) {
            ImageWmsParams imageWMSParams = OLFactory.createOptions();
            imageWMSParams.setLayers(forgroundLayer.getName());
            imageWMSParams.set("FORMAT", "image/png");
            imageWMSParams.set("TRANSPARENT", "true");
            imageWMSParams.set("VERSION", "1.3.0");

            ImageWmsOptions imageWMSOptions = OLFactory.createOptions();
            imageWMSOptions.setUrl(forgroundLayer.getBaseUrl());
            imageWMSOptions.setRatio(1.2f);
            imageWMSOptions.setParams(imageWMSParams);

            ImageWms imageWMSSource = new ImageWms(imageWMSOptions);

            LayerOptions layerOptions = OLFactory.createOptions();
            layerOptions.setSource(imageWMSSource);

            ol.layer.Image wmsLayer = new ol.layer.Image(layerOptions);
            //wmsLayer.set(ID_ATTR_NAME, id);
            double opacity = 1.0 - (forgroundLayer.getTransparency() / 100.0);
            wmsLayer.setOpacity(opacity);
            wmsLayer.setVisible(forgroundLayer.getIsVisible());
            
            map.addLayer(wmsLayer);
        }
    }
}
