package ch.so.agi.wgc;

import ch.so.agi.wgc.config.Config;
import ch.so.agi.wgc.config.ConfigManager;
import ol.Coordinate;
import ol.OLFactory;
import ol.View;
import ol.layer.LayerOptions;
import ol.layer.Tile;
import ol.proj.Projection;
import ol.source.Wmts;
import ol.source.WmtsOptions;
import ol.tilegrid.TileGrid;
import ol.tilegrid.WmtsTileGrid;
import ol.tilegrid.WmtsTileGridOptions;

public class WmtsManager {
    private static ConfigManager configManager;
    
    private Config config;
    
    private ol.Map map;

    public WmtsManager(ol.Map map) {
        this.map = map;
        configManager = ConfigManager.getInstance();
        config = configManager.getConfig();
    }
    
    public void addBasemapLayer(String layerId) {
        double resolutions[] = config.basemaps[0].resolutions;
        
        WmtsOptions wmtsOptions = OLFactory.createOptions();
        wmtsOptions.setUrl("https://geo.so.ch/api/wmts/1.0.0/{Layer}/default/2056/{TileMatrix}/{TileRow}/{TileCol}");
        wmtsOptions.setLayer(layerId);
        wmtsOptions.setRequestEncoding("REST");
        wmtsOptions.setFormat("image/png");
        wmtsOptions.setMatrixSet("EPSG:2056");
        wmtsOptions.setStyle("default");
        View view = map.getView();
        wmtsOptions.setProjection(view.getProjection());
        wmtsOptions.setWrapX(true);
        wmtsOptions.setTileGrid(createWmtsTileGrid(view.getProjection(), resolutions));

        Wmts wmtsSource = new Wmts(wmtsOptions);

        LayerOptions wmtsLayerOptions = OLFactory.createOptions();
        wmtsLayerOptions.setSource(wmtsSource);

        Tile wmtsLayer = new Tile(wmtsLayerOptions);
        wmtsLayer.setOpacity(1);
        wmtsLayer.set("basemap", true);

        map.addLayer(wmtsLayer);
    }
    
    private static TileGrid createWmtsTileGrid(Projection projection, double[] resolutions) {
        WmtsTileGridOptions wmtsTileGridOptions = OLFactory.createOptions();
        
        String[] matrixIds = new String[resolutions.length];

        for (int z = 0; z < resolutions.length; ++z) {
            matrixIds[z] = String.valueOf(z);
        }

        Coordinate tileGridOrigin = projection.getExtent().getTopLeft();
        wmtsTileGridOptions.setOrigin(tileGridOrigin);
        wmtsTileGridOptions.setResolutions(resolutions);
        wmtsTileGridOptions.setMatrixIds(matrixIds);

        return new WmtsTileGrid(wmtsTileGridOptions);
    }    
}
