package net.imagini.jzookeeperedit;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ZkTreeNode extends TreeItem<ZkNode> {
    private static final Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    private static Logger LOGGER = LoggerFactory.getLogger(ZkTreeNode.class);
    private static final Predicate<String> TRUE_PREDICATE = str -> true;

    // The depth of this tree item in the {@link TreeView}.
    private final int depth;

    // Control if the children of this tree item has been loaded.
    private boolean hasLoadedChildren = false;

    // Denote if the node is currently filtered.
    private boolean isFiltered = false;

    private final String path;
    private Stat statCache = null;

    /**
     * Create a new TreeItem which wraps a ZkNode.
     * Only lazily loads it's children, data and status, caching once retrieved for faster redraws.
     */
    public ZkTreeNode(CuratorFramework zkClient, String itemText, int depth, String path) {
        super(new ZkNode(zkClient, itemText));
        this.depth = depth;
        this.path = path.equals("/") ? "" : path;
    }

    public boolean isFiltered() {
        return isFiltered;
    }

    public void invalidateChildrenCache() {
        hasLoadedChildren = false;
    }

    public void invalidateMetadataCache() {
        this.statCache = null;
    }

    @Override
    public ObservableList<TreeItem<ZkNode>> getChildren() {
        if (!hasLoadedChildren) {
            loadChildren();
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        return statCache != null && statCache.getNumChildren() == 0;
    }

    public String getCanonicalPath() {
        return path.isEmpty() ? "/" : path;
    }

    public void loadChildren() {
        loadChildren(TRUE_PREDICATE);
        invalidateMetadataCache();
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
            super.getChildren()
                    .setAll(children.parallelStream()
                                    .filter(filterPredicate)
                                    .sorted()
                                    .peek(nodeLabel -> LOGGER.info("Adding child: {}", nodeLabel))
                                    .map(nodeLabel -> new ZkTreeNode(getClient(), nodeLabel, localDepth,
                                                                            path.concat("/").concat(nodeLabel)))
                                    .collect(Collectors.toList()));
            super.setExpanded(true);
        } catch (Exception ex) {
            hasLoadedChildren = false;
            LOGGER.error("Error while loading children", ex);
        }
    }

    private CuratorFramework getClient() {
        return super.getValue().getClient();
    }

    /**
     * @return depth of this item within the {@link TreeView}.
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get metadata for this node.
     * Will cache after retrieval and thereafter return the cache until invalidated.
     */
    public Optional<Stat> getStat() {
        return statCacheExists()
                ? Optional.ofNullable(statCache)
                : getStatFromServer(true);
    }

    private boolean statCacheExists() {
        return statCache != null;
    }

    /**
     * Get node metadata direct from the server, skipping the cache.
     * @param cacheResponse Whether to cache the server response.
     */
    public Optional<Stat> getStatFromServer(boolean cacheResponse) {
        if (getClient().getState().equals(CuratorFrameworkState.LATENT)) {
            return Optional.empty();
        }
        try {
            Stat stat = getClient().checkExists().forPath(getCanonicalPath());
            if (cacheResponse) {
                statCache = stat;
            }
            return Optional.of(stat);
        } catch (Exception ex) {
            LOGGER.error(String.format("Error while retrieving metadata for %s", getCanonicalPath()), ex);
            return Optional.empty();
        }
    }

    /**
     * Retrieve data content for the node from the server.
     */
    public Optional<String> getData() throws Exception {
        if (path.isEmpty()) {
            return Optional.empty();
        }
        try {
            byte[] data = getClient().getData().forPath(path);
            return Optional.ofNullable(data)
                           .map(bytes -> new String(bytes, CHARSET));
        } catch (Exception ex) {
            LOGGER.error(String.format("Error while retrieving data for %s", getCanonicalPath()), ex);
            throw ex;
        }
    }

    /**
     * Update the content of this node to the provided bytes.
     */
    public boolean save(byte[] bytes) {
        try {
            if (path.isEmpty()) {
                return false;
            }
            getClient().setData().forPath(path, bytes);
            return true;
        } catch (Exception ex) {
            LOGGER.error(String.format("Error while saving data for %s", getCanonicalPath()), ex);
            return false;
        }
    }
}
