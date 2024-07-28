package ch.so.agi.wgc;

import static jsinterop.annotations.JsPackage.GLOBAL;

import java.util.List;

import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = GLOBAL, name = "Object")
public class Config {
    // Für unseren Usecase müsste es nicht einmal List sein.
    public BasemapConfig[] basemaps;
    
    /*
     * Momentan nur Layer vom gleichen WMTS möglich.
     * Sonstige Config (z.B. scales) sind hardcodiert.
     */
    @JsType(isNative = true, namespace = GLOBAL, name = "Object")
    public class BasemapConfig {
        public String url;
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
