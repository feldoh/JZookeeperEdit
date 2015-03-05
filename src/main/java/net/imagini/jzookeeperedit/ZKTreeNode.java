/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.imagini.jzookeeperedit;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;

/**
 *
 * @author dlowe
 */
public class ZKTreeNode extends TreeItem<ZKNode> {

    /**
     * The depth of this tree item in the {@link TreeView}.
     */
    private final int depth;
    /**
     * Control if the children of this tree item has been loaded.
     */
    private boolean hasLoadedChildren = false;
    private final String path;
    private byte[] dataCache;

    public ZKTreeNode(CuratorFramework zkClient, String itemText, int depth, String path) {
        super(new ZKNode(zkClient, itemText));
        this.depth = depth;
        this.path = path.equals("/") ? "" : path;
    }
    
    public void setChildrenCacheIsDirty() {
        hasLoadedChildren = false;
    }
    
    @Override
    public ObservableList<TreeItem<ZKNode>> getChildren() {
        if (hasLoadedChildren == false) {
            loadChildren();
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        Optional<Stat> stat = getStat();
        return stat.isPresent() ? stat.get().getNumChildren() == 0 : false;
    }
    
    public String getCanonicalPath() {
        return path.isEmpty() ? "/" : path;
    }

    /**
     * Refresh the list of children
     */
    @SuppressWarnings("unchecked") // Safe to ignore since we know that the types are correct.
    public void loadChildren() {
        if (getClient().getState().equals(CuratorFrameworkState.LATENT)) {
            Logger.getLogger(this.getClass().getSimpleName()).log(Level.INFO,
                    "Starting client for: {0}",
                    super.getValue().toString());
            getClient().start();
        }
        
        Logger.getLogger(this.getClass().getSimpleName()).log(Level.INFO,
                    "Loading children of: {0}",
                    super.getValue().toString());
        
        hasLoadedChildren = true;
        int localDepth = depth + 1;
        try {
            List<String> children = getClient().getChildren().forPath(
                    path.isEmpty() ? "/" : path);
            super.setExpanded(false);
            super.getChildren().setAll(children.parallelStream().sorted().map((String nodeLabel) -> {
                Logger.getLogger(this.getClass().getSimpleName()).log(Level.INFO,
                    "Adding child: {0}", nodeLabel);
                return new ZKTreeNode(
                                getClient(),
                                nodeLabel,
                                localDepth,
                                path.concat("/").concat(nodeLabel));
            }).collect(Collectors.toList()));
            super.setExpanded(true);
        } catch (Exception ex) {
            Logger.getLogger(ZKTreeNode.class.getName()).log(Level.SEVERE, null, ex);
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
        if (getClient().getState().equals(CuratorFrameworkState.LATENT)){
            return Optional.empty();
        }
        try {
            return Optional.of(getClient().checkExists().forPath(getCanonicalPath()));
        } catch (Exception ex) {
            Logger.getLogger(ZKTreeNode.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ZKTreeNode.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(ZKTreeNode.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
}
