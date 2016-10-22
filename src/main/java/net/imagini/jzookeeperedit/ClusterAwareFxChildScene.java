package net.imagini.jzookeeperedit;

public interface ClusterAwareFxChildScene extends FxChildScene {
    void setZkClusterManager(ZkClusterManager clusterManager);
}
