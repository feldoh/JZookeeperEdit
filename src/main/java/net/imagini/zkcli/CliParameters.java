package net.imagini.zkcli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;
import net.imagini.jzookeeperedit.ZkClusterManager;
import org.apache.curator.framework.CuratorFramework;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CliParameters {
    @Parameter
    private List<String> positionalParameters = Lists.newArrayList();

    @Parameter(names = { "--cluster", "-c" },
            description = "Aliased Cluster Name")
    private String cluster;

    @Parameter(names = { "--zkConnect", "-z" },
            description = "Zookeeper connection string for non aliased cluster")
    private String zkConnect;

    @Parameter(names = { "--ls", "-l" },
            description = "Print child nodes")
    private boolean listChildren;

    @Parameter(names = { "--rm-recursive", "-r", "--rmr" },
            description = "Remove a node, deleting any children recursively if necessary. Requires a path and cluster")
    private boolean deleteNodeRecursive;

    @Parameter(names = { "--rm" },
            description = "Remove a node only if it does not have children. Requires a path and cluster")
    private boolean deleteNodeNonRecursive;

    @Parameter(names = { "--rm-children", "--rmc" },
            description = "Remove all children of a node recursively, keeping the specified node intact."
                    + "Requires a path and cluster")
    private boolean deleteChildrenOfNode;

    @Parameter(names = { "--printPaths", "-p" },
            description = "When printing node names include full paths")
    private boolean printPaths;

    @Parameter(names = { "--get", "-g" },
            description = "Print data of path* requires a path and cluster")
    private boolean getData;

    @Parameter(names = { "--getMeta", "-m" },
            description = "Whether to print metadata* requires a path and cluster")
    private boolean getMeta;

    @Parameter(names = { "--metaField", "-f" },
            description = "A specific field of the stat object to get. Expects getter name")
    private String specificMetaFieldGetter;

    public List<String> getPositionalParameters() {
        return positionalParameters;
    }

    public String getZkConnect() {
        return zkConnect;
    }

    public String getFriendlyName() {
        return cluster;
    }

    public boolean isListChildren() {
        return listChildren;
    }

    public boolean isDeleteNodeRecursive() {
        return deleteNodeRecursive;
    }

    public boolean isDeleteNodeNonRecursive() {
        return deleteNodeNonRecursive;
    }

    public boolean isDeleteChildrenOfNode() {
        return deleteChildrenOfNode;
    }

    public boolean isPrintPaths() {
        return printPaths;
    }

    public boolean isGetData() {
        return getData;
    }

    public boolean isGetMeta() {
        return getMeta;
    }

    public String getSpecificMetaFieldGetter() {
        return specificMetaFieldGetter;
    }

    public boolean isListMetaAccessors() {
        return listMetaAccessors;
    }

    public boolean isHelp() {
        return help;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    public ZkClusterManager getClusterManager() {
        return clusterManager;
    }

    @Parameter(names = { "--listMetaFieldAccessors", "-a" },
            description = "List accessors available for meta field access")
    private boolean listMetaAccessors;

    @Parameter(names = { "--help", "-h", "-?" },
            description = "Display this help",
            help = true)
    private boolean help;

    @Parameter(names = { "--blacklist", "--protect", "--preserve", "-b" }, variableArity = true)
    private List<String> blacklist = new ArrayList<>();


    public static final String programName = "JZookeeperEdit";
    private final JCommander argProcessor;
    private final ZkClusterManager clusterManager;

    public CliParameters(List<String> args, ZkClusterManager clusterManager) {
        this(args.toArray(new String[args.size()]), clusterManager);
    }

    /**
     * Parse the given arguments.
     * Automatically adds the "/zookeeper" node to the blacklist.
     */
    public CliParameters(String[] args, ZkClusterManager clusterManager) {
        this.clusterManager = clusterManager;
        argProcessor = new JCommander(this);
        argProcessor.setProgramName(programName);
        argProcessor.parse(args);
        blacklist.add("/zookeeper");
    }

    public boolean includesAction() {
        return listMetaAccessors || listChildren || deleteNodeRecursive || deleteNodeNonRecursive
                || deleteChildrenOfNode || getMeta || getData || help;
    }

    /**
     * Return a built zookeeper client from the config provided.
     * Note that if both -z and -c are specified -z takes precedence.
     */
    public Optional<CuratorFramework> getCluster() {
        try {
            return zkConnect != null && !zkConnect.isEmpty()
                           ? clusterManager.buildClient(zkConnect)
                           : clusterManager.getClient(cluster);
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    void printUsage() {
        argProcessor.usage();
    }
}
