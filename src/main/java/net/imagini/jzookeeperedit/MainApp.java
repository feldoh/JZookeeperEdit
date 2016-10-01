package net.imagini.jzookeeperedit;

import javafx.application.Application;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.imagini.zkcli.CliParameters;
import net.imagini.zkcli.ZkCli;

import java.util.Optional;


public class MainApp extends Application {
    private Stage primaryStage;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        super.init();
        preInit().ifPresent(Runtime.getRuntime()::exit);
        this.primaryStage = primaryStage;
        setup();
    }

    /**
     * Check if we are running a CLI command, if so execute it and exit, otherwise delegate to UI setup.
     * @return A unix exit code if execution should end; otherwise empty.
     */
    private Optional<Integer> preInit() {
        final Parameters rawParams = getParameters();
        CliParameters params = new CliParameters(rawParams.getRaw().toArray(new String[rawParams.getRaw().size()]));
        if (params.includesAction()) {
            try {
                new ZkCli(params).run();
            } catch (Exception ex) {
                //Avoid spamming logs in CLI mode
                System.err.println(ex.getMessage());
                return Optional.of(1);
            }
            return Optional.of(0);
        }
        return Optional.empty();
    }

    private void setup() {
        FxSceneManager mainContainer = new FxSceneManager();
        mainContainer.setScene(FxSceneManager.Scene.SERVER_BROWSER);
        
        AnchorPane root = new AnchorPane();
        root.getChildren().addAll(mainContainer); 
        javafx.scene.Scene scene = new javafx.scene.Scene(root);

        if (primaryStage == null) {
            throw new IllegalStateException("Primary stage not set - Unable to display scene on primary primaryStage.");
        } else {
            primaryStage.setScene(scene);
            primaryStage.show();
        }
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
