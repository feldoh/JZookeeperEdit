package net.imagini.jzookeeperedit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;

/**
 *
 * @author dlowe
 */
public class FXSceneManager extends StackPane {

    private final Map<SCENE, Node> scenes = new HashMap<>();

    public static enum SCENE {
        SERVER_BROWSER
    }
    
    public FXSceneManager() {
        init();
    }
    
    private void init() {
        AnchorPane.setBottomAnchor(this, 0.0D);
        AnchorPane.setTopAnchor(this, 0.0D);
        AnchorPane.setLeftAnchor(this, 0.0D);
        AnchorPane.setRightAnchor(this, 0.0D);
    }

    public boolean loadScreen(SCENE scene, String resource) {
        try {
            FXMLLoader myLoader = new FXMLLoader(getClass().getResource(resource));
            Parent loadScreen = (Parent) myLoader.load();
            FXChildScene myScreenControler
                = ((FXChildScene) myLoader.getController());
            myScreenControler.setFXSceneManager(this);
            loadScreen.getStylesheets().add("/styles/Styles.css");
            addScreen(scene, loadScreen);
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public void addScreen(SCENE scene, Node screen) {
        scenes.put(scene, screen);
    }

    public boolean setScene(final SCENE scene) {
        // If there is already a screen loaded
        if (scenes.get(scene) != null) {
            //If there is an existing scene remove it first
            if (!getChildren().isEmpty()) {
                //remove displayed screen
                getChildren().remove(0);
            }

            //add new screen
            getChildren().add(0, scenes.get(scene));
            return true;
        } else {
            System.out.println("screen hasn't been loaded!\n");
            return false;
        }
    }
}
