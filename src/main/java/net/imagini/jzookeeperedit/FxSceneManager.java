package net.imagini.jzookeeperedit;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FxSceneManager extends StackPane {
    private static final Logger LOGGER = LoggerFactory.getLogger(FxSceneManager.class);
    private static final String STYLESHEET_PATH = "/styles/Styles.css";

    private final Map<Scene, Node> scenes = new HashMap<>();

    enum Scene {
        SERVER_BROWSER("/fxml/ServerBrowser.fxml");

        private final String fxmlPath;

        Scene(String fxmlPath) {
            this.fxmlPath = fxmlPath;
        }

        FXMLLoader buildLoader() {
            return new FXMLLoader(getClass().getResource(fxmlPath));
        }
    }
    
    FxSceneManager() {
        init();
    }
    
    private void init() {
        AnchorPane.setBottomAnchor(this, 0.0D);
        AnchorPane.setTopAnchor(this, 0.0D);
        AnchorPane.setLeftAnchor(this, 0.0D);
        AnchorPane.setRightAnchor(this, 0.0D);
    }

    private Node loadScreen(Scene scene) {
        try {
            FXMLLoader myLoader = scene.buildLoader();
            Parent loadScreen = myLoader.load();
            FxChildScene myScreenControler = myLoader.getController();
            myScreenControler.setFxSceneManager(this);
            loadScreen.getStylesheets().add(STYLESHEET_PATH);
            return loadScreen;
        } catch (IOException e) {
            LOGGER.error(String.format("Error loading Scene %s", scene.toString()), e);
            return null;
        }
    }

    boolean setScene(final Scene scene) {
        Node screen = scenes.computeIfAbsent(scene, this::loadScreen);
        // If there is already a screen loaded
        if (screen != null) {
            //If there is an existing Scene remove it first
            if (!getChildren().isEmpty()) {
                //remove displayed screen
                getChildren().remove(0);
            }

            //add new screen
            getChildren().add(0, screen);
            return true;
        } else {
            LOGGER.error("Could not load Scene {}", scene.toString());
            return false;
        }
    }
}
