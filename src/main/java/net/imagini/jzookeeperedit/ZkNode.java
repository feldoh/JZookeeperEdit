package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;

public class ZkNode {
    private final CuratorFramework zkClient;
    private String label;
    
    public ZkNode(CuratorFramework zookeeperClient, String nodeLabel) {
        this.zkClient = zookeeperClient;
        this.label = nodeLabel;
    }
    
    public final void setLabel(String nodeLabel) {
        this.label = nodeLabel;
    }
    
    public final CuratorFramework getClient() {
        return zkClient;
    }
    
    @Override
    public final String toString() {
        return label;
    }
}
