package ch.so.agi.wgc;

import ch.so.agi.wgc.config.ConfigManager;
import ol.Coordinate;
import ol.Extent;
import ol.OLFactory;
import ol.View;
import ol.ViewOptions;
import ol.proj.Projection;
import ol.proj.ProjectionOptions;
import proj4.Proj4;

public class ViewManager {    
    private static ConfigManager configManager;
    
    private View view;
    
    public ViewManager(ol.Map map) {
        configManager = ConfigManager.getInstance();
        
        Proj4.defs("EPSG:2056", "+proj=somerc +lat_0=46.95240555555556 +lon_0=7.439583333333333 +k_0=1 +x_0=2600000 +y_0=1200000 +ellps=bessel +towgs84=674.374,15.056,405.346,0,0,0,0 +units=m +no_defs");
        ProjectionOptions projectionOptions = OLFactory.createOptions();
        projectionOptions.setCode("EPSG:2056");
        projectionOptions.setUnits("m");
        projectionOptions.setExtent(new Extent(2420000, 1030000, 2900000, 1350000));
        Projection projection = new Projection(projectionOptions);
        Projection.addProjection(projection);

        ViewOptions viewOptions = OLFactory.createOptions();
        viewOptions.setProjection(projection);
        viewOptions.setResolutions(new double[] { 4000.0, 2000.0, 1000.0, 500.0, 250.0, 100.0, 50.0, 20.0, 10.0, 5.0, 2.5, 1.0, 0.5, 0.25, 0.1 });
        view = new View(viewOptions);
        Coordinate centerCoordinate = new Coordinate(2616500, 1237000);
        view.setCenter(centerCoordinate);
        view.setZoom(5);        
    }
    
    public ol.View getView() {
        return view;
    }
}
