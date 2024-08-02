package ch.so.agi.wgc.config;

import static jsinterop.annotations.JsPackage.GLOBAL;

import java.util.List;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = GLOBAL, name = "Object")
public class Config {
    // Für unseren Usecase müsste es nicht einmal List sein.
    @JsProperty
    public Basemap[] basemaps;
    
    @JsProperty
    public String baseUrlWms;
    
    /*
     * Momentan nur Layer vom gleichen WMTS möglich.
     * Sonstige Config (z.B. scales) sind hardcodiert.
     */
    @JsType(isNative = true, namespace = GLOBAL, name = "Object")
    public static class Basemap {
        @JsProperty
        public String url;
        
        @JsProperty
        public double[] resolutions;
        
        @JsProperty
        public String[] layers;

//        public String getUrl() {
//            return url;
//        }
//        public void setUrl(String url) {
//            this.url = url;
//        }
//        public List<String> getLayers() {
//            return layers;
//        }
//        public void setLayers(List<String> layers) {
//            this.layers = layers;
//        }
    }
}
