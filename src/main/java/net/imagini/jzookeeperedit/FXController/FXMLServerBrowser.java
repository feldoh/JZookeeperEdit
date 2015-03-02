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
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import net.imagini.jzookeeperedit.FXChildScene;
import net.imagini.jzookeeperedit.FXSceneManager;
import net.imagini.jzookeeperedit.ZKClusterManager;
import net.imagini.jzookeeperedit.ZKNode;
import net.imagini.jzookeeperedit.ZKTreeNode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.WizardPane;
import org.controlsfx.validation.Validator;

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
                    ZKTreeNode treeNode = (ZKTreeNode) item;
                    String data = treeNode.getData();
                    if (data == null) {
                        text.setText("");
                        text.setDisable(true);
                    } else {
                        text.setText(data);
                        text.setDisable(false);
                    }
                    treeNode.loadChildren();
                }
            }
        });

        accordionData.expandedPaneProperty().addListener(
            (ObservableValue<? extends TitledPane> property,
                    final TitledPane oldPane,
                    final TitledPane newPane) -> {
                if (oldPane != null) {
                    oldPane.setCollapsible(true);
                }
                if (newPane != null) {
                    if (newPane.getId().equals("paneMetadata")) {
                        updateMetaData();
                    }
                }
            }
        );
    }
    
    private void updateMetaData() {
        TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();
        if (item instanceof ZKTreeNode) {
            Stat stat = ((ZKTreeNode) item).getStat().orElse(null);
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
        Wizard newClusterWizard = new Wizard(null, "Add new cluster");
        
        WizardPane friendlyNamePane = new WizardPane();
        friendlyNamePane.setHeaderText("Please provide a friendly name for this cluster");
        TextField txtFriendlyName = new TextField("localhost");
        txtFriendlyName.setId("friendlyName");
        newClusterWizard.getValidationSupport()
                .registerValidator(txtFriendlyName,
                        Validator.createEmptyValidator("Friendly Name is mandatory"));
        friendlyNamePane.setContent(txtFriendlyName);
        
        WizardPane zkConnectPane = new WizardPane();
        zkConnectPane.setHeaderText("Please provide your zk cluster connection string.");
        TextField txtZkConnect = new TextField("localhost:2181");
        txtZkConnect.setId("zkConnect");
        newClusterWizard.getValidationSupport()
                .registerValidator(txtZkConnect,
                        Validator.createEmptyValidator("Connection string is mandatory"));
//        newClusterWizard.getValidationSupport()
//                .registerValidator(txtZkConnect,
//                        Validator.createRegexValidator(
//                                "Your connection string must be of the format \"host1:port,host2:port\"",
//                                "(([\\\\w\\\\.\\\\-]+):(\\\\d+))(,([\\\\w\\\\.\\\\-]+):(\\\\d+))*",
//                                Severity.ERROR));
        zkConnectPane.setContent(txtZkConnect);
        
        newClusterWizard.setFlow(new Wizard.LinearFlow(friendlyNamePane, zkConnectPane));
        ButtonType result = newClusterWizard.showAndWait().orElse(null);
        if (result == ButtonType.FINISH) {
            Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                "New Cluster Wizard finished, settings were: {0}",
                newClusterWizard.getSettings());
        } else if (result == ButtonType.CANCEL) {
                Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                    "Cancelled adding cluster");
                return;
        }
        
        CuratorFramework zkClient = ZKClusterManager.addclient(
                String.valueOf(newClusterWizard.getSettings().get(txtFriendlyName.getId())),
                String.valueOf(newClusterWizard.getSettings().get(txtZkConnect.getId()))
        );

        addClusterToTree(zkClient, txtFriendlyName.getId());
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
                new Alert(Alert.AlertType.INFORMATION,
                    "Your data has been written to Zookeeper",
                    ButtonType.OK).showAndWait();
            }
        } else {
            new Alert(Alert.AlertType.ERROR,
                    "You cant save changes to data outside a zkNode",
                    ButtonType.OK).showAndWait();
        }
    }

    @FXML
    private void saveServerInfo() {
        try {
            ZKClusterManager.dumpConnectionDetails();
        } catch (IOException writeFailedException) {
            Logger.getLogger(this.getClass().getName())
                    .log(Level.SEVERE, null, writeFailedException);
            ExceptionDialog exceptionDialog = new ExceptionDialog(writeFailedException);
            exceptionDialog.setTitle("Exporting Server Details Failed");        
            exceptionDialog.setHeaderText("Unable to write config file to\n"
                    .concat(ZKClusterManager.clusterConfigFile.getAbsolutePath()));
            exceptionDialog.showAndWait();
        }
    }
}
