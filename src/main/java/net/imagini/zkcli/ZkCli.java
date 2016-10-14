package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ZkCli implements Runnable {
    private final CliParameters params;
    private final Set<String> pathBlackList = new HashSet<>();
    private final ZkDeleteHandler zkDeleteHandler;
    private final ZkReadHandler zkReadHandler;
    private final ZkMetadataHandler zkMetadataHandler;

    public ZkCli(CliParameters params) {
        this(params, new ZkDeleteHandler(), new ZkReadHandler(), new ZkMetadataHandler());
    }

    /**
     * Construct a ZkCli instance with alternate action handlers.
     */
    public ZkCli(CliParameters params,
                 ZkDeleteHandler zkDeleteHandler,
                 ZkReadHandler zkReadHandler,
                 ZkMetadataHandler zkMetadataHandler) {
        this.zkDeleteHandler = zkDeleteHandler;
        this.zkReadHandler = zkReadHandler;
        this.zkMetadataHandler = zkMetadataHandler;
        this.params = params;
        pathBlackList.addAll(params.blacklist);
    }

    @Override
    public void run() {
        if (params.listMetaAccessors) {
            zkMetadataHandler.getMetaAccessorMethodNames().forEach(System.out::println);
        } else if (params.help) {
            params.printUsage();
        } else {
            doCliActions();
        }
    }

    private void doCliActions() {
        CuratorFramework client = params.getCluster().orElseThrow(
            () -> new IllegalArgumentException("Please provide a valid connection string or cluster alias"));
        try {
            while (true) {
                try {
                    client.start();
                    if (!client.blockUntilConnected(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Could not connect to named cluster "
                                                                + params.cluster
                                                                + " within timeout. Check your connections.");
                    }
                    break;
                } catch (InterruptedException e) {
                    System.err.println("Interrupted while trying to connect, retrying");
                } catch (Throwable connectionMethodInvalidException) {
                    throw new RuntimeException("Connection Failed - Ensure connection string is valid",
                                                      connectionMethodInvalidException);
                }
            }
            System.err.println("Established connection to "
                    + (params.cluster == null ? params.zkConnect : params.cluster));
            params.positionalParameters.forEach(path -> runCliActionsForPath(client, path));
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ex) {
                    System.err.println("Failed to close client cleanly - Ignoring");
                }
            }
        }
    }

    private void runCliActionsForPath(CuratorFramework client, String path) {
        if (params.listChildren) {
            zkReadHandler.getChildren(client, path, params.printPaths).forEach(System.out::println);
        }
        if (params.getData) {
            System.out.println(zkReadHandler.getPathData(client, path));
        }
        if (params.getMeta) {
            System.out.println(zkMetadataHandler.getPathMetaData(client, path, params.specificMetaFieldGetter));
        }
        if (params.deleteNodeNonRecursive) {
            zkDeleteHandler.deleteNodeNonRecursive(client, path, pathBlackList);
        }
        if (params.deleteNodeRecursive) {
            zkDeleteHandler.deleteNodeRecursive(client, path, pathBlackList);
        }
        if (params.deleteChildrenOfNode) {
            zkDeleteHandler.deleteChildrenOfNode(client, path, pathBlackList);
        }
    }

}
