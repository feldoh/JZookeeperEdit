package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;

/**
 *
 * @author dlowe
 */
public class ZKNode {
    private final CuratorFramework zkClient;
    private String label;
    
    public ZKNode(CuratorFramework zkClient, String label) {
        this.zkClient = zkClient;
        this.label = label;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }
    
    public CuratorFramework getClient() {
        return zkClient;
    }
    
    @Override
    public String toString() {
        return label;
    }
}
