/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.imagini.jzookeeperedit.FXController;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import net.imagini.jzookeeperedit.FXChildScene;
import net.imagini.jzookeeperedit.FXSceneManager;
import net.imagini.jzookeeperedit.ZKClusterManager;
import net.imagini.jzookeeperedit.ZKNode;
import net.imagini.jzookeeperedit.ZKTreeNode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.zookeeper.data.Stat;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.WizardPane;
import org.controlsfx.validation.Validator;


public class FXMLServerBrowser implements Initializable, FXChildScene {

    private static final Logger LOGGER = Logger.getLogger(FXMLServerBrowser.class.getName());
    private static final byte[] EMPTY_BYTES = "".getBytes();
            
    private FXSceneManager fxSceneManager;

    @FXML private TextArea text;
    @FXML private Button btnAddChild;
    @FXML private Button btnAddSibling;
    @FXML private Button btnDelete;
    @FXML private Button btnSave;
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
    
    private void loadData(ZKTreeNode treeNode) {
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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        browser.setRoot(new TreeItem<>(new ZKNode(null, "Servers")));
        ZKClusterManager.getClusters().forEach((key, val) -> {
            addClusterToTree(val, key);
        });

        browser.setOnMouseClicked((MouseEvent mouseEvent) -> {
            TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();
            text.clear();
            updateValidUIOptions(item);
            updateMetaData();
            
            if (mouseEvent.getClickCount() == 2) {
                if (item instanceof ZKTreeNode) {
                    loadData((ZKTreeNode) item);
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
        FlowPane friendlyNameContentPane = new FlowPane(10, 10);
        friendlyNameContentPane.getChildren().add(txtFriendlyName);
        friendlyNamePane.setContent(friendlyNameContentPane);
        
        WizardPane zkConnectPane = new WizardPane();
        zkConnectPane.setHeaderText("Please provide your zk cluster connection string.");
        TextField txtZkConnect = new TextField("localhost:2181");
        txtZkConnect.setId("zkConnectString");
        txtZkConnect.setMinWidth(100);
        newClusterWizard.getValidationSupport()
                .registerValidator(txtZkConnect,
                        Validator.createEmptyValidator("Connection string is mandatory"));
        FlowPane zkConnectContentPane = new FlowPane(10, 10);
        zkConnectContentPane.getChildren().add(txtZkConnect);
        zkConnectPane.setContent(zkConnectContentPane);
        
        newClusterWizard.setFlow(new Wizard.LinearFlow(friendlyNamePane, zkConnectPane));
        ButtonType result = newClusterWizard.showAndWait().orElse(null);
        
        // Workaround for last page in Wizard not saving entries.
        try {
            Method wizardUpdateSettingsMethod;
            wizardUpdateSettingsMethod = Wizard.class.
                    getDeclaredMethod("readSettings", WizardPane.class);
            wizardUpdateSettingsMethod.setAccessible(true);
            wizardUpdateSettingsMethod.invoke(newClusterWizard, zkConnectPane);
        zkConnectPane.onExitingPage(newClusterWizard);
        } catch (NoSuchMethodException 
                | SecurityException 
                | IllegalAccessException 
                | IllegalArgumentException 
                | InvocationTargetException reflectionException) {
            Logger.getLogger(FXMLServerBrowser.class.getName()).log(Level.SEVERE, null, reflectionException);
            new Alert(Alert.AlertType.ERROR,
                    "Failed to grab data from wizard. Please try again",
                    ButtonType.OK).showAndWait();
            return;
        }
        
        if (result == ButtonType.FINISH) {
            LOGGER.log(Level.INFO, "New Cluster Wizard finished, settings were: {0}",
                newClusterWizard.getSettings());
        } else if (result == ButtonType.CANCEL) {
                LOGGER.log(Level.INFO, "Cancelled adding cluster");
                return;
        }
        
        String friendlyName = String.valueOf(newClusterWizard.getSettings().get(txtFriendlyName.getId()));
        String zkConnect = String.valueOf(newClusterWizard.getSettings().get(txtZkConnect.getId()));
        CuratorFramework zkClient = ZKClusterManager.addclient(friendlyName, zkConnect);
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
            LOGGER.log(Level.SEVERE, null, writeFailedException);
            ExceptionDialog exceptionDialog = new ExceptionDialog(writeFailedException);
            exceptionDialog.setTitle("Exporting Server Details Failed");        
            exceptionDialog.setHeaderText("Unable to write config file to\n"
                    .concat(ZKClusterManager.clusterConfigFile.getAbsolutePath()));
            exceptionDialog.showAndWait();
        }
    }
    
    @FXML
    private void addChild() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Child");
        dialog.setHeaderText("Please enter a name for the ZKNode");
        dialog.showAndWait().ifPresent(nodeLabel -> {
            if (nodeLabel.trim().isEmpty()){
                new Alert(Alert.AlertType.ERROR,
                    "You cant have a ZKNode without a name",
                    ButtonType.OK).showAndWait();
                return;
            }
            TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();
            if (item instanceof ZKTreeNode) {
                try {
                    ZKTreeNode treeNode = (ZKTreeNode) item;
                    String siblingPath = treeNode.getCanonicalPath();
                    StringBuilder nodePathBuilder = new StringBuilder(siblingPath);
                    if (!siblingPath.equals("/")) {
                        nodePathBuilder.append("/");
                    }
                    nodePathBuilder.append(nodeLabel);
                    item.getValue().getClient().create().forPath(nodePathBuilder.toString(), EMPTY_BYTES);
                    treeNode.loadChildren();
                } catch (Exception zookeeperClientException) {
                    LOGGER.log(Level.SEVERE, null, zookeeperClientException);
                    ExceptionDialog exceptionDialog = new ExceptionDialog(zookeeperClientException);
                    exceptionDialog.setTitle("Adding Child Node failed");        
                    exceptionDialog.setHeaderText("Failed to add: " + nodeLabel);
                    exceptionDialog.showAndWait();
                }
            }
        });
    }
    
    @FXML
    private void addSibling() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Sibling");
        dialog.setHeaderText("Please enter a name for the ZKNode");
        dialog.showAndWait().ifPresent(nodeLabel -> {
            if (nodeLabel.trim().isEmpty()){
                new Alert(Alert.AlertType.ERROR,
                    "You cant have a ZKNode without a name",
                    ButtonType.OK).showAndWait();
                return;
            }
            TreeItem<ZKNode> parent = browser.getSelectionModel().getSelectedItem().getParent();
            if (parent instanceof ZKTreeNode) {
                try {
                    ZKTreeNode treeNode = (ZKTreeNode) parent;
                    String parentPath = treeNode.getCanonicalPath();
                    StringBuilder nodePathBuilder = new StringBuilder(parentPath);
                    if (!parentPath.equals("/")) {
                        nodePathBuilder.append("/");
                    }
                    nodePathBuilder.append(nodeLabel);
                    parent.getValue().getClient().create().forPath(nodePathBuilder.toString(), EMPTY_BYTES);
                    treeNode.loadChildren();
                } catch (Exception zookeeperClientException) {
                    LOGGER.log(Level.SEVERE, null, zookeeperClientException);
                    ExceptionDialog exceptionDialog = new ExceptionDialog(zookeeperClientException);
                    exceptionDialog.setTitle("Adding Sibling Node failed");        
                    exceptionDialog.setHeaderText("Failed to add: " + nodeLabel);
                    exceptionDialog.showAndWait();
                }
            }
        });
    }
    
    @FXML
    private void deleteNode() {
        TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();
        if (item instanceof ZKTreeNode) {
            ZKTreeNode treeNode = (ZKTreeNode) item;
            if (treeNode.getCanonicalPath().equals("/")){
                new Alert(Alert.AlertType.ERROR,
                    "You cant have a ZKNode without a name",
                    ButtonType.OK).showAndWait();
                return;
            }
            Alert confirmDelete = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDelete.getButtonTypes().clear();
            confirmDelete.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            confirmDelete.setTitle("Deleting Node");
            confirmDelete.setHeaderText("Deleting " + treeNode.getCanonicalPath());
            confirmDelete.setContentText("Are you sure you want to delete this node and all of its children");
            confirmDelete.showAndWait().ifPresent(buttonType -> {
                if (buttonType.equals(ButtonType.YES)){
                    try {
                        treeNode.getValue().getClient().delete().guaranteed()
                                .deletingChildrenIfNeeded().forPath(treeNode.getCanonicalPath());
                        browser.getSelectionModel().selectPrevious();
                        TreeItem<ZKNode> deletedNodeParent = treeNode.getParent();
                        if (deletedNodeParent instanceof ZKTreeNode) {
                            ZKTreeNode parentTreeNode = (ZKTreeNode) deletedNodeParent;
                            parentTreeNode.setChildrenCacheIsDirty();
                            parentTreeNode.getChildren().remove(item);
                            new Alert(Alert.AlertType.INFORMATION,
                            MessageFormat.format("Removed node {0}", treeNode.getCanonicalPath()),
                                ButtonType.OK).showAndWait();
                            parentTreeNode.loadChildren();
                            updateMetaData();
                        }
                    } catch (Exception zookeeperClientException) {
                        LOGGER.log(Level.SEVERE, null, zookeeperClientException);
                        ExceptionDialog exceptionDialog = new ExceptionDialog(zookeeperClientException);
                        exceptionDialog.setTitle("Deleting Node failed");        
                        exceptionDialog.setHeaderText("Failed to delete: " + treeNode.getCanonicalPath());
                        exceptionDialog.showAndWait();
                    }
                }
            });
        }
    }

    private void updateValidUIOptions(TreeItem<ZKNode> item) {
        if (item instanceof ZKTreeNode) {
            ZKTreeNode treeNode = (ZKTreeNode) item;
            boolean isClusterNode = treeNode.getDepth() == 0;
            btnAddSibling.setDisable(isClusterNode);
            btnDelete.setDisable(isClusterNode);
            btnSave.setDisable(isClusterNode);
        }
    }
}
