package net.imagini.jzookeeperedit;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.imagini.zkcli.CliParameters;
import net.imagini.zkcli.ZkCli;


public class MainApp extends Application {
    private Stage stage;
    
    @Override 
    public void start(Stage primaryStage) throws Exception {
        super.init();
        preInit();
        this.stage = primaryStage;
        setup();
    }

    /**
     * Check if we are running a CLI command, if so execute it and exit, otherwise delegate to UI setup
     */
    private void preInit() {
        final Parameters rawParams = getParameters();
        CliParameters params = new CliParameters(rawParams.getRaw().toArray(new String[rawParams.getRaw().size()]));
        if (params.includesAction()) {
            try {
                new ZkCli(params).run();
            } catch (Exception ex) {
                //Avoid spamming logs in CLI mode
                System.err.println(ex.getMessage());
                System.exit(1);
            }
            System.exit(0);
        }
    }

    private void setup() {
        FXSceneManager mainContainer = new FXSceneManager();
        mainContainer.setScene(FXSceneManager.SCENE.SERVER_BROWSER);
        
        AnchorPane root = new AnchorPane();
        root.getChildren().addAll(mainContainer); 
        Scene scene = new Scene(root); 
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
