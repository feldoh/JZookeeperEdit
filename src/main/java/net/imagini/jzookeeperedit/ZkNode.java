package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;

import java.util.Objects;

public class ZkNode {
    private final CuratorFramework zkClient;
    private String label;
    
    public ZkNode(CuratorFramework zookeeperClient, String nodeLabel) {
        this.zkClient = zookeeperClient;
        setLabel(nodeLabel);
    }
    
    public final void setLabel(String nodeLabel) {
        this.label = Objects.requireNonNull(nodeLabel, "Node with null label would not be traversable");
    }
    
    public final CuratorFramework getClient() {
        return zkClient;
    }
    
    @Override
    public final String toString() {
        return label;
    }
}
