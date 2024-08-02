package ch.so.agi.wgc.components.wgclink;

import org.jboss.elemento.Attachable;
import org.jboss.elemento.IsElement;

import ch.so.agi.wgc.models.WmsLayer;
import ch.so.agi.wgc.state.StateManager;
import elemental2.dom.HTMLElement;
import elemental2.dom.MutationRecord;

import static org.jboss.elemento.Elements.*;

import java.util.List;

public class WgcLinkComponent implements IsElement<HTMLElement>, Attachable {
    private HTMLElement root;

    private StateManager stateManager;

    public WgcLinkComponent() {
        stateManager = StateManager.getInstance();
//        List<WmsLayer> foregroundLayer = (List<WmsLayer>) stateManager.getState(StateManager.PARAM_ACTIVE_FOREGROUND_LAYERS);
        
        String imageUrl = "favicon-16x16.png";        
        root = div().css("wgcLinkContainer")
                .add(img().attr("src", imageUrl).attr("width", "16"))
                .add(span().add(a().css("wgcLink").attr("href", "https://geo.so.ch/map").attr("target", "_blank").add(span().textContent("In geo.so.ch/map ansehen"))))
                .element();
    }
    
    @Override
    public void attach(MutationRecord mutationRecord) {
    }

    @Override
    public HTMLElement element() {
        return root;
    }

}
