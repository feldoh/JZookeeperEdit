package net.imagini.zkcli;

import java.util.List;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

public class CliParameters {
    @Parameter
    public List<String> parameters = Lists.newArrayList();

    @Parameter(names = { "--cluster", "-c" }, description = "Aliased Cluster Name")
    public String cluster;

    @Parameter(names = { "--zkConnect", "-z" }, description = "Zookeeper connection string for non aliased cluster")
    public String zkConnect;

    @Parameter(names = { "--ls", "-l" }, description = "Print child nodes")
    public boolean listChildren;

    @Parameter(names = { "--printPaths", "-p" }, description = "When printing node names include full paths")
    public boolean printPaths;

    @Parameter(names = { "--get", "-g" }, description = "Print data of path* requires a path and cluster")
    public boolean getData;

    @Parameter(names = { "--getMeta", "-m" }, description = "Whether to print metadata* requires a path and cluster")
    public boolean getMeta;

    @Parameter(names = { "--metaField", "-f" }, description = "A specific field of the stat object to get. Expects getter name")
    public String specificMetaFieldGetter;

    @Parameter(names = { "--listMetaFieldAccessors", "-a" }, description = "List accessors available for meta field access")
    public boolean listMetaAccessors;

    @Parameter(names = { "--help", "-h", "-?" }, help = true, description = "Display this help")
    public boolean help;

    private final JCommander jCommander;

    public CliParameters(String[] args) {
        jCommander = new JCommander(this, args);
    }

    public boolean includesAction() {
        return listMetaAccessors || listChildren || getMeta || getData || help;
    }

    public void printUsage() {
        jCommander.usage();
    }
}
