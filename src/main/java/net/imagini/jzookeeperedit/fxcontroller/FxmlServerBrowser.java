package net.imagini.jzookeeperedit.fxcontroller;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import net.imagini.jzookeeperedit.ClusterAwareFxChildScene;
import net.imagini.jzookeeperedit.FxSceneManager;
import net.imagini.jzookeeperedit.ZkClusterManager;
import net.imagini.jzookeeperedit.ZkNode;
import net.imagini.jzookeeperedit.ZkTreeNode;
import net.imagini.jzookeeperedit.fxview.ZkTreeNodeCellFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.zookeeper.data.Stat;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.Wizard;
import org.controlsfx.dialog.WizardPane;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class FxmlServerBrowser implements Initializable, ClusterAwareFxChildScene {

    private static final Logger LOGGER = LoggerFactory.getLogger(FxmlServerBrowser.class);
    private static final Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    private static final byte[] EMPTY_BYTES = "".getBytes(CHARSET);

    private ZkClusterManager clusterManager;
    private FxSceneManager fxSceneManager;

    @FXML private TextArea text;
    @FXML private Button btnAddSibling;
    @FXML private Button btnDelete;
    @FXML private Button btnSave;
    @FXML private TreeView<ZkNode> browser;
    @FXML private Accordion accordionData;
    @FXML private TitledPane paneData;

    @FXML private TitledPane paneMetadata;
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
    public void setFxSceneManager(FxSceneManager fxSceneManager) {
        this.fxSceneManager = fxSceneManager;
    }

    @Override
    public void setZkClusterManager(ZkClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        refreshClusters();
    }

    private void refreshClusters() {
        if (clusterManager != null) {
            clusterManager.getClusters().forEach(this::addClusterToTree);
        }
    }

    private void loadData(ZkTreeNode treeNode) {
        try {
            text.setText(treeNode.getData().orElse(""));
        } catch (Exception e) {
            showError("Failed to read node content");
        }
        text.setDisable(false);
        treeNode.loadChildren();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        browser.setCellFactory(new ZkTreeNodeCellFactory());
        browser.setRoot(new TreeItem<>(new ZkNode(null, "Servers")));

        browser.setOnMouseClicked((MouseEvent mouseEvent) -> {
            TreeItem<ZkNode> item = getSelectedItem();
            text.clear();
            updateValidUiOptions(item);
            updateMetaData();

            paneData.setText("Data - Not Loaded, double click node to load");
            if (mouseEvent.getClickCount() == 2) {
                if (item instanceof ZkTreeNode) {
                    loadData((ZkTreeNode) item);
                    paneData.setText(String.format("Data - Loaded at %s",
                            LocalDateTime.now().toString()));
                }
            }
        });

        accordionData.expandedPaneProperty().addListener((ObservableValue<? extends TitledPane> property,
             final TitledPane oldPane,
             final TitledPane newPane) -> {
                if (oldPane != null) {
                    oldPane.setCollapsible(true);
                }
                if (newPane != null && paneMetadata == newPane) {
                    updateMetaData();
                }
            }
        );
    }

    private void updateMetaData() {
        TreeItem<ZkNode> item = getSelectedItem();
        if (item instanceof ZkTreeNode) {
            Stat stat = ((ZkTreeNode) item).getStat().orElse(null);
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

    /**
     * Later versions of JavaFX8 prevent validators working when set imperatively.
     * See https://bitbucket.org/controlsfx/controlsfx/issues/539/multiple-dialog-fields-with-validation
     */
    private void addLazyValidator(ValidationSupport validationSupport, Control control, Validator<?> validator) {
        Platform.runLater(() -> validationSupport.registerValidator(control, validator));
    }

    private static class SetMethodAccessiblePrivilegedAction implements PrivilegedAction<Void> {
        private final Method method;

        SetMethodAccessiblePrivilegedAction(Method method) {
            this.method = method;
        }

        public Void run() {
            method.setAccessible(true);
            return null;
        }
    }

    @FXML
    void addServer(ActionEvent event) {
        final ValidationSupport validationSupport = new ValidationSupport();

        WizardPane friendlyNamePane = new WizardPane();
        friendlyNamePane.setHeaderText("Please provide a friendly name for this cluster");
        TextField txtFriendlyName = new TextField("localhost");
        txtFriendlyName.setId("friendlyName");
        txtFriendlyName.setMinWidth(50);
        addLazyValidator(validationSupport, txtFriendlyName,
                Validator.createEmptyValidator("Friendly Name is mandatory"));
        FlowPane friendlyNameContentPane = new FlowPane(10, 10);
        friendlyNameContentPane.getChildren().add(txtFriendlyName);
        friendlyNamePane.setContent(friendlyNameContentPane);

        WizardPane zkConnectPane = new WizardPane();
        zkConnectPane.setHeaderText("Please provide your zk cluster connection string.");
        TextField txtZkConnect = new TextField("localhost:2181");
        txtZkConnect.setId("zkConnectString");
        txtZkConnect.setMinWidth(100);
        addLazyValidator(validationSupport, txtZkConnect,
                Validator.createEmptyValidator("Connection string is mandatory"));
        FlowPane zkConnectContentPane = new FlowPane(10, 10);
        zkConnectContentPane.getChildren().add(txtZkConnect);
        zkConnectPane.setContent(zkConnectContentPane);

        Wizard newClusterWizard = new Wizard(null, "Add new cluster");
        newClusterWizard.setFlow(new Wizard.LinearFlow(friendlyNamePane, zkConnectPane));
        ButtonType result = newClusterWizard.showAndWait().orElse(null);

        // Workaround for last page in Wizard not saving entries.
        // TODO: Switch away from buggy Wizards
        try {
            Method wizardUpdateSettingsMethod;
            wizardUpdateSettingsMethod = Wizard.class.getDeclaredMethod("readSettings", WizardPane.class);
            AccessController.doPrivileged(new SetMethodAccessiblePrivilegedAction(wizardUpdateSettingsMethod));
            wizardUpdateSettingsMethod.invoke(newClusterWizard, zkConnectPane);
            zkConnectPane.onExitingPage(newClusterWizard);
        } catch (NoSuchMethodException
                | SecurityException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException reflectionException) {
            LOGGER.error("Attempt to access Wizard internal settings failed.", reflectionException);
            new Alert(Alert.AlertType.ERROR,
                    "Failed to grab data from wizard. Please try again",
                    ButtonType.OK).showAndWait();
            return;
        }

        if (result == ButtonType.FINISH) {
            LOGGER.info("New Cluster Wizard finished, settings were: {}", newClusterWizard.getSettings());
        } else if (result == ButtonType.CANCEL) {
            LOGGER.info("Cancelled adding cluster");
            return;
        }

        String friendlyName = String.valueOf(newClusterWizard.getSettings().get(txtFriendlyName.getId()));
        String zkConnect = String.valueOf(newClusterWizard.getSettings().get(txtZkConnect.getId()));
        clusterManager.addclient(friendlyName, zkConnect)
                                            .ifPresent(client -> addClusterToTree(friendlyName, client));
    }

    private void addClusterToTree(String friendlyName, CuratorFramework zkClient) {
        TreeItem<ZkNode> root = browser.getRoot();
        root.getChildren().add(new ZkTreeNode(
                zkClient,
                friendlyName,
                true,
                "/"
            )
        );
        root.setExpanded(true);
    }

    @FXML
    void save() {
        TreeItem<ZkNode> item = getSelectedItem();
        if (item instanceof ZkTreeNode) {
            if (((ZkTreeNode) item).save(text.getText().getBytes(CHARSET))) {
                showInfo("Your data has been written to Zookeeper");
            } else {
                showError("Unable to save data to the selected node");
            }
        } else {
            showError("You cant save changes to data outside a zkNode");
        }
    }

    private void showError(String message) {
        new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait();
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK).showAndWait();
    }

    @FXML
    void saveServerInfo() {
        try {
            clusterManager.dumpConnectionDetails();
        } catch (IOException writeFailedException) {
            LOGGER.error("Failed to write server details to disk", writeFailedException);
            ExceptionDialog exceptionDialog = new ExceptionDialog(writeFailedException);
            exceptionDialog.setTitle("Exporting Server Details Failed");
            exceptionDialog.setHeaderText("Unable to write config file to\n"
                    .concat(clusterManager.getConfigFilePath()));
            exceptionDialog.showAndWait();
        }
    }

    @FXML
    void addChild() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Child");
        dialog.setHeaderText("Please enter a name for the ZkNode");
        dialog.showAndWait().ifPresent(nodeLabel -> {
            if (nodeLabel.trim().isEmpty()) {
                new Alert(Alert.AlertType.ERROR,
                    "You cant have a ZkNode without a name",
                    ButtonType.OK).showAndWait();
                return;
            }
            TreeItem<ZkNode> item = getSelectedItem();
            if (item instanceof ZkTreeNode) {
                try {
                    performAddChildNode((ZkTreeNode) item, nodeLabel);
                } catch (Exception zookeeperClientException) {
                    LOGGER.error("Adding Child Node failed", zookeeperClientException);
                    ExceptionDialog exceptionDialog = new ExceptionDialog(zookeeperClientException);
                    exceptionDialog.setTitle("Adding Child Node failed");
                    exceptionDialog.setHeaderText("Failed to add: " + nodeLabel);
                    exceptionDialog.showAndWait();
                }
            }
        });
    }

    @FXML
    void addSibling() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Sibling");
        dialog.setHeaderText("Please enter a name for the ZkNode");
        dialog.showAndWait().ifPresent(nodeLabel -> {
            if (nodeLabel.trim().isEmpty()) {
                new Alert(Alert.AlertType.ERROR,
                    "You cant have a ZkNode without a name",
                    ButtonType.OK).showAndWait();
                return;
            }
            TreeItem<ZkNode> parent = getSelectedItem().getParent();
            if (parent instanceof ZkTreeNode) {
                try {
                    performAddChildNode((ZkTreeNode) parent, nodeLabel);
                } catch (Exception zookeeperClientException) {
                    LOGGER.error("Adding Sibling Node failed", zookeeperClientException);
                    ExceptionDialog exceptionDialog = new ExceptionDialog(zookeeperClientException);
                    exceptionDialog.setTitle("Adding Sibling Node failed");
                    exceptionDialog.setHeaderText("Failed to add: " + nodeLabel);
                    exceptionDialog.showAndWait();
                }
            }
        });
    }

    private void performAddChildNode(ZkTreeNode parentNode, String childNodeName) throws Exception {
        String parentPath = parentNode.getCanonicalPath();
        StringBuilder nodePathBuilder = new StringBuilder(parentPath);
        if (!parentPath.equals("/")) {
            nodePathBuilder.append("/");
        }
        nodePathBuilder.append(childNodeName);
        parentNode.getValue().getClient().create().forPath(nodePathBuilder.toString(), EMPTY_BYTES);
        parentNode.loadChildren();
    }

    @FXML
    void deleteNode() {
        TreeItem<ZkNode> item = getSelectedItem();
        if (item instanceof ZkTreeNode) {
            ZkTreeNode treeNode = (ZkTreeNode) item;
            if (treeNode.getCanonicalPath().equals("/")) {
                new Alert(Alert.AlertType.ERROR,
                    "You cant delete the ZK root",
                    ButtonType.OK).showAndWait();
                return;
            }
            Alert confirmDelete = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDelete.getButtonTypes().clear();
            confirmDelete.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            confirmDelete.setTitle("Deleting Node");
            confirmDelete.setHeaderText("Deleting " + treeNode.getCanonicalPath());
            confirmDelete.setContentText("Are you sure you want to delete this node,\nand all of its children");
            confirmDelete.showAndWait()
                    .filter(ButtonType.YES::equals)
                    .ifPresent((yes) -> performDelete(treeNode));
        }
    }

    private void performDelete(ZkTreeNode treeNode) {
        try {
            treeNode.getValue().getClient().delete().guaranteed()
                    .deletingChildrenIfNeeded().forPath(treeNode.getCanonicalPath());
            browser.getSelectionModel().selectPrevious();
            TreeItem<ZkNode> deletedNodeParent = treeNode.getParent();
            if (deletedNodeParent instanceof ZkTreeNode) {
                ZkTreeNode parentTreeNode = (ZkTreeNode) deletedNodeParent;
                parentTreeNode.invalidateChildrenCache();
                parentTreeNode.getChildren().remove(treeNode);
                new Alert(Alert.AlertType.INFORMATION,
                        String.format("Removed node %s", treeNode.getCanonicalPath()),
                        ButtonType.OK).showAndWait();
                parentTreeNode.loadChildren();
                FxmlServerBrowser.this.updateMetaData();
            }
        } catch (Exception zookeeperClientException) {
            LOGGER.error("Deleting Node failed", zookeeperClientException);
            ExceptionDialog exceptionDialog = new ExceptionDialog(zookeeperClientException);
            exceptionDialog.setTitle("Deleting Node failed");
            exceptionDialog.setHeaderText("Failed to delete: " + treeNode.getCanonicalPath());
            exceptionDialog.showAndWait();
        }
    }

    @FXML
    void doFilterChildren() {
        TextInputDialog regexDialog = new TextInputDialog(".*");
        regexDialog.setTitle("Filter Node Children");
        regexDialog.setHeaderText("Please provide a regex to filter children by");
        regexDialog.showAndWait().ifPresent(this::filterChildrenOfSelectedNode);
    }

    private void filterChildrenOfSelectedNode(String regexString) {
        try {
            Pattern regex = Pattern.compile(regexString);
            TreeItem<ZkNode> item = getSelectedItem();
            if (item instanceof ZkTreeNode) {
                ((ZkTreeNode) item).loadChildren(regex.asPredicate());
            }
        } catch (PatternSyntaxException invalidRegexException) {
            LOGGER.error(String.format("Pattern '%s' is not a valid regex", regexString), invalidRegexException);
        }
    }

    private static class FilteredNodeRemovalConfirmationWizardPane extends WizardPane {
        private final ListView<String> condemmedList;
        private final String filterFieldId;

        FilteredNodeRemovalConfirmationWizardPane(ListView<String> condemmedList, String filterFieldId) {
            this.condemmedList = condemmedList;
            this.filterFieldId = filterFieldId;
        }

        @Override
        public void onEnteringPage(Wizard wizard) {
            Pattern regex = Pattern.compile(String.valueOf(wizard.getSettings().get(filterFieldId)));
            condemmedList.setItems(condemmedList.getItems().filtered(regex.asPredicate()));
            this.setHeaderText(String.format(
                    "Confirm that you want to delete this list of %d child nodes from the currently selected node",
                    condemmedList.getItems().size()));
        }
    }

    @FXML
    void deleteChildrenWithFilter() {
        WizardPane filterPane = new WizardPane();
        filterPane.setHeaderText("Please provide a regex to filter children by.\n"
                + "You will have an opportunity to back out");

        TextField txtFilter = new TextField(".*");
        txtFilter.setId("filterRegex");

        FlowPane filterContentPane = new FlowPane(10.0D, 10.0D);
        filterContentPane.getChildren().add(txtFilter);
        filterPane.setContent(filterContentPane);

        // Confirm list
        TreeItem<ZkNode> item = getSelectedItem();

        ListView<String> childrenToBeRemoved = new ListView<>(FXCollections
                .observableList(item
                    .getChildren()
                    .parallelStream()
                    .map((TreeItem<ZkNode> node) -> node.getValue().toString())
                    .collect(Collectors.toList())));

        childrenToBeRemoved.setId("condemnedChildren");
        GridPane.setHgrow(childrenToBeRemoved, Priority.ALWAYS);
        GridPane.setVgrow(childrenToBeRemoved, Priority.ALWAYS);
        GridPane confirmationContentPane = new GridPane();
        confirmationContentPane.add(childrenToBeRemoved, 0, 0);
        WizardPane confirmationPane = new FilteredNodeRemovalConfirmationWizardPane(childrenToBeRemoved,
                                                                                           txtFilter.getId());
        confirmationPane.setContent(confirmationContentPane);

        Wizard deleteBatchWizard = new Wizard(null, "Delete a group of children");
        deleteBatchWizard.setFlow(new Wizard.LinearFlow(filterPane, confirmationPane));
        deleteBatchWizard.showAndWait().ifPresent(button -> {
            if (button == ButtonType.FINISH) {
                if (item instanceof ZkTreeNode) {
                    Object lock = new Object();
                    ZkTreeNode parentTreeNode = (ZkTreeNode) item;
                    AtomicLong outstandingDeletions = new AtomicLong(childrenToBeRemoved.getItems().size());
                    childrenToBeRemoved.getItems().forEach(label -> {
                        try {
                            parentTreeNode.getValue().getClient().delete().guaranteed()
                                    .deletingChildrenIfNeeded()
                                    .inBackground((CuratorFramework cf, CuratorEvent ce) -> {
                                        if (ce.getType().equals(CuratorEventType.DELETE)) {
                                            LOGGER.info("Result code {} while deleting: {}",
                                                    String.valueOf(ce.getResultCode()), ce.getPath());
                                            synchronized (lock) {
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
                            LOGGER.error("Failed to delete all marked nodes", deletionException);
                        }
                    });
                    parentTreeNode.invalidateChildrenCache();
                    new Alert(Alert.AlertType.INFORMATION,
                            "Scheduled all nodes for removal. Please wait while they are removed.",
                            ButtonType.OK).showAndWait();
                    while (true) {
                        try {
                            synchronized (lock) {
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
                            LOGGER.info("Interrupted during deletion", ex);
                        }
                    }
                }
            }
        });
    }

    @FXML
    void doUnfilterChildren() {
        TreeItem<ZkNode> selectedItem = getSelectedItem();
        if (selectedItem instanceof ZkTreeNode) {
            loadData((ZkTreeNode) selectedItem);
        }
        browser.getSelectionModel().select(selectedItem);
    }

    private String getPathFromNodeWithClusterPrefix(TreeItem<ZkNode> selectedItem) {
        return selectedItem == null
                ? ""
                : clusterManager.getFriendlyName(selectedItem.getValue().getClient()).orElse("")
                + getPathFromNode(selectedItem);
    }

    private String getPathFromNode(TreeItem<ZkNode> node) {
        return node == null
                ? ""
                : node instanceof ZkTreeNode
                    ? ((ZkTreeNode) node).getCanonicalPath()
                    : node.getValue().toString();
    }

    @FXML
    void doGoto() {
        TextInputDialog dialog = new TextInputDialog(getPathFromNodeWithClusterPrefix(getSelectedItem()));
        dialog.setTitle("Goto Node");
        dialog.setHeaderText("Jump to another node anywhere in the tree");
        dialog.setContentText("Target path");
        dialog.showAndWait().ifPresent(this::navigateTo);
    }

    private TreeItem<ZkNode> getSelectedItem() {
        return browser.getSelectionModel().getSelectedItem();
    }

    private void navigateTo(String path) {
        String[] pathElements = path.split("/");
        int rootOffset = pathElements[0].isEmpty() ? 1 : 0;
        TreeItem<ZkNode> root = getChild(browser.getRoot(), pathElements[rootOffset]);
        if (root != null) {
            browser.getSelectionModel().select(Arrays.stream(pathElements)
                    .skip(rootOffset + 1)
                    .reduce(root, this::getChild, (parent, child) -> child));
        }
    }

    private TreeItem<ZkNode> getChild(TreeItem<ZkNode> parent, String pathElement) {
        if (parent != null) {
            Optional<TreeItem<ZkNode>> matchingChild = parent.getChildren().stream()
                     .filter((node) -> pathElement.equals(node.getValue().toString())).findFirst();
            matchingChild.ifPresent((node) -> node.setExpanded(true));
            return matchingChild.orElse(null);
        }
        return null;
    }

    private void updateValidUiOptions(TreeItem<ZkNode> item) {
        if (item instanceof ZkTreeNode) {
            ZkTreeNode treeNode = (ZkTreeNode) item;
            boolean isClusterNode = treeNode.isRoot();
            btnAddSibling.setDisable(isClusterNode);
            btnDelete.setDisable(isClusterNode);
            btnSave.setDisable(isClusterNode);
        }
    }
}
