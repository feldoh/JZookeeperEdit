package net.imagini.jzookeeperedit.fx;

import net.imagini.jzookeeperedit.ZkClusterManager;
import net.imagini.jzookeeperedit.fx.FxChildScene;

public interface ClusterAwareFxChildScene extends FxChildScene {
    void setZkClusterManager(ZkClusterManager clusterManager);
}
