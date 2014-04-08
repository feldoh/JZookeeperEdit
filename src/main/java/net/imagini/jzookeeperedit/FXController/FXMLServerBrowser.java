/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.imagini.jzookeeperedit.FXController;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javax.naming.OperationNotSupportedException;
import net.imagini.jzookeeperedit.FXChildScene;
import net.imagini.jzookeeperedit.FXSceneManager;
import net.imagini.jzookeeperedit.ZKClusterManager;
import net.imagini.jzookeeperedit.ZKNode;
import net.imagini.jzookeeperedit.ZKTreeNode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.controlsfx.dialog.Dialogs;

/**
 *
 * @author dlowe
 */
public class FXMLServerBrowser implements Initializable, FXChildScene {

    private FXSceneManager fxSceneManager;

    @FXML private TextArea text;
    @FXML private TreeView<ZKNode> browser;
    @FXML private Accordion accordionData;
    
    @FXML private Label labcZxid;
    @FXML private Label labctime;
    @FXML private Label labmZxid;
    @FXML private Label labmtime;
    @FXML private Label labpZxid;
    @FXML private Label labcversion;
    @FXML private Label labdataversion;
    @FXML private Label labaclVersion;
    @FXML private Label labephemeralOwner;
    @FXML private Label labdataLength;
    @FXML private Label labnumChildren;

    @Override
    public void setFXSceneManager(FXSceneManager fxSceneManager) {
        this.fxSceneManager = fxSceneManager;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        browser.setRoot(new TreeItem<>(new ZKNode(null, "Servers")));
        ZKClusterManager.getClusters().forEach((key, val) -> {
            addClusterToTree(val, key);
        });

        browser.setOnMouseClicked((MouseEvent mouseEvent) -> {
            text.clear();
            updateMetaData();
            if (mouseEvent.getClickCount() == 2) {
                TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();

                if (item instanceof ZKTreeNode) {
                    if (((ZKTreeNode) item).getValue().getClient()
                            .getState().equals(CuratorFrameworkState.LATENT)) {
                        ((ZKTreeNode) item).loadChildren();
                    }
                    String data = ((ZKTreeNode) item).getData();
                    if (data == null) {
                        text.setText("");
                        text.setDisable(true);
                    } else {
                        text.setText(data);
                        text.setDisable(false);
                    }
                }
            }
        });

        accordionData.expandedPaneProperty().addListener(new ChangeListener<TitledPane>() {
            @Override
            public void changed(ObservableValue<? extends TitledPane> property, final TitledPane oldPane, final TitledPane newPane) {
                if (oldPane != null) {
                    oldPane.setCollapsible(true);
                }
                if (newPane != null) {
                    if (newPane.getId().equals("paneMetadata")) {
                        updateMetaData();
                    }
                }
            }
        });
    }
    
    private void updateMetaData() {
        TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();
        if (item instanceof ZKTreeNode) {
            if (((ZKTreeNode) item).getValue().getClient()
                    .getState().equals(CuratorFrameworkState.LATENT)) {
                ((ZKTreeNode) item).loadChildren();
            }
            Stat stat = ((ZKTreeNode) item).getStat();
            if (stat != null) {
                labaclVersion.setText(String.valueOf(stat.getAversion()));
                labcZxid.setText(String.valueOf(stat.getCzxid()));
                labctime.setText(String.valueOf(stat.getCtime()));
                labmZxid.setText(String.valueOf(stat.getMzxid()));
                labmtime.setText(String.valueOf(stat.getMtime()));
                labpZxid.setText(String.valueOf(stat.getPzxid()));
                labcversion.setText(String.valueOf(stat.getCversion()));
                labdataversion.setText(String.valueOf(stat.getVersion()));
                labephemeralOwner.setText(String.valueOf(stat.getEphemeralOwner()));
                labdataLength.setText(String.valueOf(stat.getDataLength()));
                labnumChildren.setText(String.valueOf(stat.getNumChildren()));
            }
        }
    }

    @FXML
    private void addServer(ActionEvent event) {
        String friendlyName = getServerInfo(
                "Please provide a friendly name for this cluster",
                "localhost");

        CuratorFramework zkClient = ZKClusterManager.addclient(
                friendlyName,
                getServerInfo(
                        "Please provide your zk cluster connection string.",
                        "localhost:2181"
                )
        );

        addClusterToTree(zkClient, friendlyName);
    }

    private void addClusterToTree(CuratorFramework zkClient, String friendlyName) {
        browser.getRoot().getChildren().add(new ZKTreeNode(
                zkClient,
                friendlyName,
                0,
                "/"
        )
        );
    }

    @FXML
    private void save() {
        TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();

        if (item instanceof ZKTreeNode) {
            if (((ZKTreeNode) item).save(text.getText().getBytes())) { //TODO: Enforce charset
                Dialogs.create()
                        .owner(null)
                        .title("Operation Complete")
                        .masthead("Success")
                        .message("Your data has been written to Zookeeper")
                        .showInformation();
            }
        } else {
            Dialogs.create()
                    .owner(null)
                    .title("Invalid Save Target")
                    .showException(new OperationNotSupportedException(
                                    "You cant save changes to data outside a zkNode"));
        }
    }

    private String getServerInfo(String message, String defaultValue) {
        return Dialogs.create()
                .owner(null)
                .title("Server information needed")
                .masthead(message)
                .showTextInput(defaultValue);
    }

    @FXML
    private void saveServerInfo() {
        try {
            ZKClusterManager.dumpConnectionDetails();
        } catch (IOException ex) {
            Logger.getLogger(FXMLServerBrowser.class.getName()).log(Level.SEVERE, null, ex);
            Dialogs.create()
                    .owner(null)
                    .title("Exporting Server Details Failed")
                    .showException(new OperationNotSupportedException(
                                    "Unable to write config file to\n".concat(
                                            ZKClusterManager.clusterConfigFile.getAbsolutePath())));
        }
    }
}
