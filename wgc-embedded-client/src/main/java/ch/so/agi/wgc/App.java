package ch.so.agi.wgc;

import static elemental2.dom.DomGlobal.console;
import static org.jboss.elemento.Elements.*;

import com.google.gwt.core.client.EntryPoint;

import org.dominokit.domino.ui.themes.DominoThemeManager;
import org.dominokit.domino.ui.themes.DominoThemeAccent;
import org.dominokit.domino.ui.themes.DominoThemeLight;

import org.dominokit.domino.ui.forms.TextBox;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class App implements EntryPoint {

    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        DominoThemeManager.INSTANCE.apply(DominoThemeLight.INSTANCE);
        DominoThemeManager.INSTANCE.apply(DominoThemeAccent.BLUE); 

        console.log("Hallo Welt.");

        body().add(TextBox.create().setLabel("User name")
                                .setPlaceholder("Username").element());

    }
}