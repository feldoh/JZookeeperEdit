/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.imagini.jzookeeperedit;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

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
            final DoubleProperty opacity = opacityProperty();

            //Is there is more than one screen 
            if (!getChildren().isEmpty()) {
                Timeline fade = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(opacity, 1.0D)),
                        new KeyFrame(new Duration(1000),
                                new EventHandler() {
                                    @Override
                                    public void handle(Event t) {
                                        //remove displayed screen 
                                        getChildren().remove(0);
                                        //add new screen 
                                        getChildren().add(0, scenes.get(scene));
                                        Timeline fadeIn = new Timeline(
                                                new KeyFrame(Duration.ZERO,
                                                        new KeyValue(opacity, 0.0D)),
                                                new KeyFrame(new Duration(800),
                                                        new KeyValue(opacity, 1.0D)));
                                        fadeIn.play();
                                    }
                                }, new KeyValue(opacity, 0.0D)));
                fade.play();
            } else {
                //no one else been displayed, then just show 
                setOpacity(0.0D);
                getChildren().add(scenes.get(scene));
                Timeline fadeIn = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(opacity, 0.0D)),
                        new KeyFrame(new Duration(2500),
                                new KeyValue(opacity, 1.0D)));
                fadeIn.play();
            }
            return true;
        } else {
            System.out.println("screen hasn't been loaded!\n");
            return false;
        }
    }
}
