package ch.so.agi.wgc.models;

public class FeatureInfoResponse {
    private String layerName;
    
    private String layerTitle;
    
    private String htmlContent;
    
    private String reportUrl;
    
    public FeatureInfoResponse() {}
    
    public FeatureInfoResponse(String layerName, String layerTitle, String htmlContent, String reportUrl) {
        this.layerName = layerName;
        this.layerTitle = layerTitle;
        this.htmlContent = htmlContent;
        this.reportUrl = reportUrl;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getLayerTitle() {
        return layerTitle;
    }

    public void setLayerTitle(String layerTitle) {
        this.layerTitle = layerTitle;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public String getReportUrl() {
        return reportUrl;
    }

    public void setReportUrl(String reportUrl) {
        this.reportUrl = reportUrl;
    }  
}
