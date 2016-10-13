package net.imagini.zkcli;

import com.google.common.annotations.VisibleForTesting;

import net.imagini.jzookeeperedit.ZkClusterManager;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

public class ZkCli implements Runnable {
    private static final Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    private final CliParameters params;
    private Set<String> pathBlackList = new HashSet<>();

    /**
     * Constructor for ZkCli. Initializes pathBlackList and automatically adds
     * the "/zookeeper" path to the blacklist.
     * @param params the command line parameters object
     */
    public ZkCli(CliParameters params) {
        this.params = params;
        // TODO: expose way to add to the blacklist
        pathBlackList.add("/zookeeper");
    }

    @Override
    public void run() {
        if (params.listMetaAccessors) {
            printMetaAccessors();
        } else if (params.help) {
            params.printUsage();
        } else {
            doCliActions();
        }
    }

    private void doCliActions() {
        CuratorFramework client = getCluster(params).orElseThrow(
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
            params.positionalParameters.forEach(path -> {
                if (params.listChildren) {
                    printChildren(client, path, params.printPaths);
                }
                if (params.getData) {
                    printPathData(client, path);
                }
                if (params.getMeta) {
                    printPathMetaData(client, path, params.specificMetaFieldGetter);
                }
                if (params.deleteNodeNonRecursive) {
                    deleteNodeNonRecursive(client, path);
                }
                if (params.deleteNodeRecursive) {
                    deleteNodeRecursive(client, path);
                }
                if (params.deleteChildrenOfNode) {
                    deleteChildrenOfNode(client, path);
                }
            });
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

    private Optional<CuratorFramework> getCluster(CliParameters cliParameters) {
        try {
            return Optional.ofNullable(cliParameters.cluster == null || cliParameters.cluster.isEmpty()
                    ? ZkClusterManager.buildClient(cliParameters.zkConnect)
                    : ZkClusterManager.getClient(cliParameters.cluster));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private void printPathData(CuratorFramework client, String path) {
        try {
            System.out.println(new String(client.getData().forPath(path), CHARSET));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printPathMetaData(CuratorFramework client, String path, String metadataItemGetterName) {
        try {
            System.out.println(formatMetaData(client.checkExists().forPath(path), metadataItemGetterName));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void printMetaAccessors() {
        try {
            Arrays.stream(Stat.class.getDeclaredMethods())
                    .filter(method -> method.getParameterCount() == 0)
                    .map(Method::getName)
                    .forEach(System.out::println);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String formatMetaData(Stat stat, String metadataItemGetterName) {
        try {
            return metadataItemGetterName == null || metadataItemGetterName.isEmpty()
                    ? stat.toString()
                    : Stat.class.getDeclaredMethod(metadataItemGetterName)
                    .invoke(stat)
                    .toString();
        } catch (RuntimeException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private String formatNodeName(String path, String child, boolean includePath) {
        StringJoiner joiner = new StringJoiner(path.equals("/") ? "" : "/");
        return (includePath ? joiner.add(path) : joiner)
                .add(child).toString();
    }

    private void printChildren(CuratorFramework client, String path, boolean printPaths) {
        try {
            client.getChildren().forPath(path).stream()
                    .map(String::new)
                    .map(child -> formatNodeName(path, child, printPaths))
                    .forEach(System.out::println);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    void deleteNodeNonRecursive(CuratorFramework client, String path) {
        try {
            client.delete().forPath(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    void deleteNodeRecursive(CuratorFramework client, String path) {
        try {
            client.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    void deleteChildrenOfNode(CuratorFramework client, String path) {
        try {
            client.getChildren().forPath(path).stream()
                    .map(c -> path.endsWith("/") ? path + c : path + "/" + c)
                    .filter(fullPath -> !pathBlackList.contains(fullPath))
                    .forEach(fullPath -> deleteNodeRecursive(client, fullPath));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
