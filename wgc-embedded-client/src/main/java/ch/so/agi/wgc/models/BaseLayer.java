package ch.so.agi.wgc.models;

public abstract class BaseLayer {
    protected String name;
    
    protected String baseUrl;
    
    protected boolean isVisible;
    
    protected int transparency;
    
    protected boolean isExternal;
    
    public BaseLayer(String name, String baseUrl, boolean isVisible, int transparency, boolean isExternal) {
        this.name = name;
        this.baseUrl = baseUrl;
        this.isVisible = isVisible;
        this.transparency = transparency;
        this.isExternal = isExternal;
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

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean isVisible) {
        this.isVisible = isVisible;
    }

    public int getTransparency() {
        return transparency;
    }

    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }

    public boolean isExternal() {
        return isExternal;
    }

    public void setExternal(boolean isExternal) {
        this.isExternal = isExternal;
    }
}
