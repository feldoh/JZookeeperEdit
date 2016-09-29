package net.imagini.jzookeeperedit;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dlowe
 */
public class ZKTreeNode extends TreeItem<ZKNode> {
    private static Logger LOGGER = LoggerFactory.getLogger(ZKTreeNode.class);
    private static final Predicate<String> TRUE_PREDICATE = str -> true;

    /**
     * The depth of this tree item in the {@link TreeView}.
     */
    private final int depth;
    /**
     * Control if the children of this tree item has been loaded.
     */
    private boolean hasLoadedChildren = false;
    private boolean isFiltered = false;
    private final String path;
    private byte[] dataCache;
    private Optional<Stat> statCache;

    public ZKTreeNode(CuratorFramework zkClient, String itemText, int depth, String path) {
        super(new ZKNode(zkClient, itemText));
        this.depth = depth;
        this.path = path.equals("/") ? "" : path;
        this.statCache = Optional.empty();
    }

    public boolean isFiltered() {
        return isFiltered;
    }

    public void setChildrenCacheIsDirty() {
        hasLoadedChildren = false;
    }

    @Override
    public ObservableList<TreeItem<ZKNode>> getChildren() {
        if (!hasLoadedChildren) {
            loadChildren();
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        Optional<Stat> stat = getStat();
        return stat.isPresent() && stat.get().getNumChildren() == 0;
    }

    public String getCanonicalPath() {
        return path.isEmpty() ? "/" : path;
    }

    public void loadChildren() {
        loadChildren(TRUE_PREDICATE);
        this.statCache = Optional.empty();
    }

    /**
     * Refresh the list of children
     * @param filterPredicate A predicate for a filter which is applied to the stream of children.
     */
    public void loadChildren(Predicate<String> filterPredicate) {
        isFiltered = !filterPredicate.equals(TRUE_PREDICATE);

        if (getClient().getState().equals(CuratorFrameworkState.LATENT)) {
            LOGGER.info("Starting client for: {}", super.getValue().toString());
            getClient().start();
        }

        LOGGER.info("Loading children of: {}", super.getValue().toString());
        hasLoadedChildren = true;
        int localDepth = depth + 1;
        try {
            List<String> children = getClient().getChildren().forPath(
                    path.isEmpty() ? "/" : path);
            super.setExpanded(false);
            super.getChildren().setAll(children.parallelStream()
                    .filter(filterPredicate)
                    .sorted()
                    .map((String nodeLabel) -> {
                        LOGGER.info("Adding child: {}", nodeLabel);
                        return new ZKTreeNode(
                                getClient(),
                                nodeLabel,
                                localDepth,
                                path.concat("/").concat(nodeLabel));
            }).collect(Collectors.toList()));
            super.setExpanded(true);
        } catch (Exception ex) {
            hasLoadedChildren = false;
            LOGGER.error("Error while loading children", ex);
        }
    }

    private CuratorFramework getClient() {
        return super.getValue().getClient();
    }

    /* @return depth of this item within the {@link TreeView}.*/
    public int getDepth() {
        return depth;
    }

    public Optional<Stat> getStat() {
        if (!statCache.isPresent()) {
            statCache = getStatFromServer();
        }
        return statCache;
    }

    public Optional<Stat> getStatFromServer() {
        if (getClient().getState().equals(CuratorFrameworkState.LATENT)){
            return Optional.empty();
        }
        try {
            return Optional.of(getClient().checkExists().forPath(getCanonicalPath()));
        } catch (Exception ex) {
            LOGGER.error(String.format("Error while retrieving metadata for %s", getCanonicalPath()), ex);
            return Optional.empty();
        }
    }

    public String getData() {
        if (path.isEmpty()) {
            return "";
        }
        try {
            dataCache = getClient().getData().forPath(path);
            return dataCache == null ? "" : new String(dataCache);
        } catch (Exception ex) {
            LOGGER.error(String.format("Error while retrieving data for %s", getCanonicalPath()), ex);
            return "";//TODO pop up an error
        }
    }

    public boolean save(byte[] bytes) {
        try {
            if (path.isEmpty()) {
                return false; //TODO Show error, cant save outside a ZK node
            }
            getClient().setData().forPath(path, bytes);
            return true;
        } catch (Exception ex) {
            LOGGER.error(String.format("Error while saving data for %s", getCanonicalPath()), ex);
            return false;
        }
    }
}
