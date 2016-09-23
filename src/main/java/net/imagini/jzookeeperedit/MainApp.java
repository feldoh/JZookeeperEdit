package net.imagini.jzookeeperedit;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import net.imagini.zkcli.CliParameters;

import com.beust.jcommander.JCommander;


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
        CliParameters params = new CliParameters();
        new JCommander(params, rawParams.getRaw().toArray(new String[rawParams.getRaw().size()]));
        if (params.cluster != null) {
            try(CuratorFramework client = ZKClusterManager.getClient(params.cluster)) {
                while(true) {
                    try {
                        client.start();
                        if (!client.blockUntilConnected(10, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Could not connect to named cluster " + params.cluster + " within timeout. Check your connections.");
                        }
                        break;
                    } catch (InterruptedException e) {
                        System.err.println("Interrupted while trying to connect, retrying");
                    }
                }
                System.err.println("Established connection to " + params.cluster);
                if (params.getData) {
                    params.parameters.forEach(path -> printPathData(client, path));
                }
                if (params.getChildData) {
                    params.parameters.forEach(path -> {
                        try {
                            client.getChildren()
                                    .forPath(path)
                                    .forEach(child -> printPathData(client, path + "/" + child));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                throw new IllegalStateException("Error encountered during CLI operation", e);
            }
            System.exit(0);
        }
    }

        private void printPathData(CuratorFramework client, String path) {
            try {
                System.out.println(new String(client.getData().forPath(path)));
            } catch (Exception e) {
                throw new RuntimeException(e);
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
