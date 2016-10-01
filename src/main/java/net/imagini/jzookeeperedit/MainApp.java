package net.imagini.jzookeeperedit;

import javafx.application.Application;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;


public class MainApp extends Application {
    private Stage stage;
    
    @Override 
    public void start(Stage primaryStage) { 
        this.stage = primaryStage;
        setup();
    }
    
    private void setup() {
        FxSceneManager mainContainer = new FxSceneManager();
        mainContainer.setScene(FxSceneManager.Scene.SERVER_BROWSER);
        
        AnchorPane root = new AnchorPane();
        root.getChildren().addAll(mainContainer); 
        javafx.scene.Scene scene = new javafx.scene.Scene(root);
        stage.setScene(scene); 
        stage.show(); 
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
