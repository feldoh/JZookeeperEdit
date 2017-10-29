package net.imagini.jzookeeperedit;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ZkTreeNode extends TreeItem<ZkNode> {
    private static final Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    private static final Logger LOGGER = LoggerFactory.getLogger(ZkTreeNode.class);
    private static final Predicate<String> TRUE_PREDICATE = str -> true;

    // The isRoot of this tree item in the {@link TreeView}.
    private final boolean isRoot;

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
    public ZkTreeNode(CuratorFramework zkClient, String itemText, boolean isRoot, String path) {
        super(new ZkNode(zkClient, itemText));
        this.isRoot = isRoot;
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

    public String getCanonicalPath() {
        return path.isEmpty() ? "/" : path;
    }

    private CuratorFramework ensureActive(CuratorFramework curatorFramework) {
        if (curatorFramework.getState().equals(CuratorFrameworkState.LATENT)) {
            LOGGER.info("Starting client for: {}", super.getValue().toString());
            curatorFramework.start();
        }
        return curatorFramework;
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
        CuratorFramework client = ensureActive(getClient());
        isFiltered = !filterPredicate.equals(TRUE_PREDICATE);

        LOGGER.info("Loading children of: {}", super.getValue().toString());
        hasLoadedChildren = true;
        try {
            List<String> children = client.getChildren().forPath(
                    path.isEmpty() ? "/" : path);
            super.setExpanded(false);
            super.getChildren()
                    .setAll(children.parallelStream()
                                    .filter(filterPredicate)
                                    .sorted()
                                    .peek(nodeLabel -> LOGGER.info("Adding child: {}", nodeLabel))
                                    .map(nodeLabel -> new ZkTreeNode(client, nodeLabel, false,
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
     * @return true if the node is the root of a zookeeper tree.
     */
    public boolean isRoot() {
        return isRoot;
    }

    /**
     * Get metadata for this node.
     * Will cache after retrieval and thereafter return the cache until invalidated.
     */
    public Optional<Stat> getStat() {
        return statCacheExists()
                ? Optional.of(statCache)
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
        CuratorFramework client = ensureActive(getClient());
        if (client.getState().equals(CuratorFrameworkState.LATENT)) {
            return Optional.empty();
        }
        try {
            Stat stat = client.checkExists().forPath(getCanonicalPath());
            if (cacheResponse) {
                statCache = stat;
            }
            return Optional.ofNullable(stat);
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
            byte[] data = ensureActive(getClient()).getData().forPath(path);
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
            ensureActive(getClient()).setData().forPath(path, bytes);
            return true;
        } catch (Exception ex) {
            LOGGER.error(String.format("Error while saving data for %s", getCanonicalPath()), ex);
            return false;
        }
    }

    /**
     * Save content of the node using the default charset.
     */
    public boolean save(String data) {
        return save(data.getBytes(CHARSET));
    }

    @Override
    public ObservableList<TreeItem<ZkNode>> getChildren() {
        if (!hasLoadedChildren) {
            loadChildren();
        }
        return super.getChildren();
    }

    /**
     * Check if the current node is a leaf.
     * If we're at the root and the connection is closed we assume no.
     * This avoids opening potentially invalid connections.
     */
    @Override
    public boolean isLeaf() {
        // Until the connection is started we assume there might be some children to avoid opening extra connections.
        if (!getClient().getState().equals(CuratorFrameworkState.STARTED) && getCanonicalPath().equals("/")) {
            return false;
        }
        return getStat()
                       .map(Stat::getNumChildren)
                       .orElse(0) == 0;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || !(that instanceof ZkTreeNode)) {
            return false;
        }

        ZkTreeNode zkTreeNode = (ZkTreeNode) that;

        return getValue().equals(zkTreeNode.getValue())
                       && path.equals(zkTreeNode.path);
    }

    @Override
    public int hashCode() {
        int result = (isRoot() ? 1 : 0);
        result = 31 * result + path.hashCode();
        result = 31 * result + getValue().hashCode();
        return result;
    }
}
