package net.imagini.jzookeeperedit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dlowe
 */
public class FXSceneManager extends StackPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(FXSceneManager.class);
    private static final String STYLESHEET_PATH = "/styles/Styles.css";

    private final Map<SCENE, Node> scenes = new HashMap<>();

    enum SCENE {
        SERVER_BROWSER("/fxml/ServerBrowser.fxml");

        private final String fxmlPath;

        SCENE(String fxmlPath) {
            this.fxmlPath = fxmlPath;
        }

        FXMLLoader buildLoader() {
            return new FXMLLoader(getClass().getResource(fxmlPath));
        }
    }
    
    FXSceneManager() {
        init();
    }
    
    private void init() {
        AnchorPane.setBottomAnchor(this, 0.0D);
        AnchorPane.setTopAnchor(this, 0.0D);
        AnchorPane.setLeftAnchor(this, 0.0D);
        AnchorPane.setRightAnchor(this, 0.0D);
    }

    private Node loadScreen(SCENE scene) {
        try {
            FXMLLoader myLoader = scene.buildLoader();
            Parent loadScreen = myLoader.load();
            FXChildScene myScreenControler = myLoader.getController();
            myScreenControler.setFXSceneManager(this);
            loadScreen.getStylesheets().add(STYLESHEET_PATH);
            return loadScreen;
        } catch (IOException e) {
            LOGGER.error(String.format("Error loading scene %s", scene.toString()), e);
            return null;
        }
    }

    private void addScreen(SCENE scene, Node screen) {
        scenes.put(scene, screen);
    }

    boolean setScene(final SCENE scene) {
        Node screen = scenes.computeIfAbsent(scene, this::loadScreen);
        // If there is already a screen loaded
        if (screen != null) {
            //If there is an existing scene remove it first
            if (!getChildren().isEmpty()) {
                //remove displayed screen
                getChildren().remove(0);
            }

            //add new screen
            getChildren().add(0, screen);
            return true;
        } else {
            LOGGER.error("Could not load scene {}", scene.toString());
            return false;
        }
    }
}
