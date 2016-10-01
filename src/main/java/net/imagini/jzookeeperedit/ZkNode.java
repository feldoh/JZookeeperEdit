package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;

public class ZkNode {
    private final CuratorFramework zkClient;
    private String label;
    
    public ZkNode(CuratorFramework zkClient, String label) {
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
