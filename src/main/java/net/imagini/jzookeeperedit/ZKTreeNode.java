/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.imagini.jzookeeperedit;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;

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
    
    @Override
    public ObservableList<TreeItem<ZKNode>> getChildren() {
        if (super.getValue().getClient()
                        .getState().equals(CuratorFrameworkState.STARTED)) {
            if (hasLoadedChildren == false) {
                loadChildren();
            }
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (super.getValue().getClient()
                        .getState().equals(CuratorFrameworkState.LATENT)) {
            return true;
        }
        if (hasLoadedChildren == false) {
            loadChildren();
        }
        return super.getChildren().isEmpty();
    }

    /**
     * Create some dummy children for this item.
     */
    @SuppressWarnings("unchecked") // Safe to ignore since we know that the types are correct.
    public void loadChildren() {
        if (super.getValue().getClient()
                .getState().equals(CuratorFrameworkState.LATENT)) {
            super.getValue().getClient().start();
        }
        hasLoadedChildren = true;
        int localDepth = depth + 1;
        try {
            super.getValue().getClient().getChildren().forPath(
                    path.isEmpty() ? "/" : path).forEach((String s) -> {
                        super.getChildren().add(
                                new ZKTreeNode(
                                        super.getValue().getClient(),
                                        s,
                                        localDepth,
                                        path.concat("/").concat(s)));
                    });
        } catch (Exception ex) {
            Logger.getLogger(ZKTreeNode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /* @return depth of this item within the {@link TreeView}.*/
    public int getDepth() {
        return depth;
    }

    public String getData() {
        if (path.isEmpty()) {
            return "";
        }
        try {
            dataCache = super.getValue().getClient().getData().forPath(path);
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
            super.getValue().getClient().setData().forPath(path, bytes);
            return true;
        } catch (Exception ex) {
            Logger.getLogger(ZKTreeNode.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
}
