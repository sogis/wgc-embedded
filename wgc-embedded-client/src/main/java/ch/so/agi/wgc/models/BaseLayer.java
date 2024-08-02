package ch.so.agi.wgc.models;

public abstract class BaseLayer {
    protected String name;
    
    protected String baseUrl;
    
    protected boolean isVisible;
    
    protected int transparency;
    
    public BaseLayer(String name, String baseUrl, boolean isVisible, int transparency) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.isVisible = isVisible;
        this.transparency = transparency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean getIsVisible() {
        return isVisible;
    }

    public void setIsVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public int getTransparency() {
        return transparency;
    }

    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }
}
