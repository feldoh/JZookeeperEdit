package net.imagini.jzookeeperedit.FXController;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Paint;
import javafx.util.Callback;
import net.imagini.jzookeeperedit.FXChildScene;
import net.imagini.jzookeeperedit.FXSceneManager;
import net.imagini.jzookeeperedit.ZKClusterManager;
import net.imagini.jzookeeperedit.ZKNode;
import net.imagini.jzookeeperedit.ZKTreeNode;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.zookeeper.data.Stat;
import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.control.decoration.Decorator;
import org.controlsfx.control.decoration.StyleClassDecoration;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.Wizard.WizardPane;
import org.controlsfx.validation.Validator;


public class FXMLServerBrowser implements Initializable, FXChildScene {

    private static final Logger LOGGER = Logger.getLogger(FXMLServerBrowser.class.getName());
    private static final byte[] EMPTY_BYTES = "".getBytes();
    private static final Decoration FILTERED_DECORATION = new StyleClassDecoration(
            "tree-cell-filtered");

    private FXSceneManager fxSceneManager;

    @FXML private TextArea text;
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
        browser.setCellFactory(new Callback<TreeView<ZKNode>, TreeCell<ZKNode>>() {
            @Override
            public TreeCell<ZKNode> call(TreeView<ZKNode> param) {
                return new TreeCell<ZKNode>() {
                    @Override
                    protected void updateItem(ZKNode item, boolean empty) {
                        super.updateItem(item, empty);

                        if (!empty) {
                            if (this.getTreeItem() instanceof ZKTreeNode) {
                                if (((ZKTreeNode) this.getTreeItem()).isFiltered()) {
                                    Decorator.addDecoration(this, FILTERED_DECORATION);
                                } else {
                                    Decorator.removeDecoration(this, FILTERED_DECORATION);
                                }
                            }
                            setTextIfChanged(item.toString());
                        } else {
                            this.setText(null);
                            this.setGraphic(null);
                        }
                    }

                    private void setTextIfChanged(String newText) {
                        if (hasStringChanged(this.getText(), newText)) {
                            this.setText(newText);
                        }
                    }

                    private boolean hasStringChanged(String oldString, String newString) {
                        LOGGER.log(Level.FINEST, "Rendered TreeCell: {0} -> {1}",
                                new String[]{String.valueOf(oldString), String.valueOf(newString)});
                        return oldString == null ? newString != null : !oldString.equals(newString);
                    }
                };
            }
        });
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
            LOGGER.log(Level.SEVERE, "Attempt to access Wizard internal settings failed.", reflectionException);
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
            LOGGER.log(Level.SEVERE, "Failed to write server details to disk", writeFailedException);
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
                    LOGGER.log(Level.SEVERE, "Adding Child Node failed", zookeeperClientException);
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
                    LOGGER.log(Level.SEVERE, "Adding Sibling Node failed", zookeeperClientException);
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
                        LOGGER.log(Level.SEVERE, "Deleting Node failed", zookeeperClientException);
                        ExceptionDialog exceptionDialog = new ExceptionDialog(zookeeperClientException);
                        exceptionDialog.setTitle("Deleting Node failed");
                        exceptionDialog.setHeaderText("Failed to delete: " + treeNode.getCanonicalPath());
                        exceptionDialog.showAndWait();
                    }
                }
            });
        }
    }

    @FXML
    private void doFilterChildren() {
        TextInputDialog regexDialog = new TextInputDialog(".*");
        regexDialog.setTitle("Filter Node Children");
        regexDialog.setHeaderText("Please provide a regex to filter children by");
        regexDialog.showAndWait().ifPresent(regexString -> {
            Pattern regex = Pattern.compile(regexString);
            TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();
            if (item instanceof ZKTreeNode) {
                //item.getParent().setExpanded(false);
                ((ZKTreeNode) item).loadChildren(regex.asPredicate());
                //item.getParent().setExpanded(true);
            }
        });
    }

    @FXML
    private void deleteChildrenWithFilter() {
        Wizard deleteBatchWizard = new Wizard(null, "Delete a group of children");

        WizardPane filterPane = new WizardPane();
        filterPane.setHeaderText("Please provide a regex to filter children by.\n"
                + "You will have an opportunity to back out");

        TextField txtFilter = new TextField(".*");
        txtFilter.setId("filterRegex");

        FlowPane filterContentPane = new FlowPane(10.0D, 10.0D);
        filterContentPane.getChildren().add(txtFilter);
        filterPane.setContent(filterContentPane);

        // Confirm list
        TreeItem<ZKNode> item = browser.getSelectionModel().getSelectedItem();

        ListView<String> childrenToBeRemoved = new ListView<>(FXCollections
                .observableList(item
                    .getChildren()
                    .parallelStream()
                    .map((TreeItem<ZKNode> node) -> node.getValue().toString())
                    .collect(Collectors.toList())));

        WizardPane confirmationPane = new WizardPane(){
            @Override
            public void onEnteringPage(Wizard wizard) {
                Pattern regex = Pattern.compile(String.valueOf(wizard.getSettings()
                        .get(txtFilter.getId())));
                childrenToBeRemoved.setItems(childrenToBeRemoved
                        .getItems().filtered(regex.asPredicate()));
                this.setHeaderText(MessageFormat.format(
                        "Confirm that you want to delete this list of {0} child nodes from the currently selected node",
                        childrenToBeRemoved.getItems().size()));
            }
        };
        GridPane confirmationContentPane = new GridPane();
        childrenToBeRemoved.setId("condemnedChildren");
        GridPane.setHgrow(childrenToBeRemoved, Priority.ALWAYS);
        GridPane.setVgrow(childrenToBeRemoved, Priority.ALWAYS);
        confirmationContentPane.add(childrenToBeRemoved, 0, 0);
        confirmationPane.setContent(confirmationContentPane);

        deleteBatchWizard.setFlow(new Wizard.LinearFlow(filterPane, confirmationPane));
        deleteBatchWizard.showAndWait().ifPresent(button -> {
            if (button == ButtonType.FINISH) {
                if (item instanceof ZKTreeNode) {
                    Object lock = new Object();
                    ZKTreeNode parentTreeNode = (ZKTreeNode) item;
                    AtomicLong outstandingDeletions = new AtomicLong(childrenToBeRemoved.getItems().size());
                    childrenToBeRemoved.getItems().forEach(label -> {
                        try {
                            parentTreeNode.getValue().getClient().delete().guaranteed()
                                    .deletingChildrenIfNeeded()
                                    .inBackground((CuratorFramework cf, CuratorEvent ce) -> {
                                        if (ce.getType().equals(CuratorEventType.DELETE)) {
                                            LOGGER.log(Level.INFO, "Result code {0} while deleting: {1}",
                                                    new String[]{String.valueOf(ce.getResultCode()), ce.getPath()});
                                            synchronized(lock) {
                                                if (outstandingDeletions.decrementAndGet() == 0) {
                                                    lock.notifyAll();
                                                }
                                            }
                                        }
                                    }).forPath(parentTreeNode.getCanonicalPath()
                                            .concat(parentTreeNode.getCanonicalPath().equals("/")
                                                    ? label
                                                    : "/".concat(label)));
                        } catch (Exception deletionException) {
                            LOGGER.log(Level.SEVERE, "Failed to delete all marked nodes", deletionException);
                        }
                    });
                    parentTreeNode.setChildrenCacheIsDirty();
                    new Alert(Alert.AlertType.INFORMATION,
                            "Scheduled all nodes for removal. Please wait while they are removed.",
                            ButtonType.OK).showAndWait();
                    while (true) {
                        try {
                            synchronized(lock) {
                                if (outstandingDeletions.get() > 0) {
                                    lock.wait();
                                } else {
                                    new Alert(Alert.AlertType.INFORMATION,
                                        "All nodes removed, thankyou for waiting",
                                        ButtonType.OK).showAndWait();
                                    parentTreeNode.loadChildren();
                                    break;
                                }
                            }
                        } catch (InterruptedException ex) {
                            LOGGER.log(Level.INFO, "Interrupted during deletion", ex);
                        }
                    }
                }
            }
        });
    }

    @FXML
    private void doUnfilterChildren() {
        TreeItem<ZKNode> selectedItem = browser.getSelectionModel().getSelectedItem();
        if (selectedItem instanceof ZKTreeNode) {
            loadData((ZKTreeNode) selectedItem);
        }
        browser.getSelectionModel().select(selectedItem);
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
