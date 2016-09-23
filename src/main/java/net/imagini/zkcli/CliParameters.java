package net.imagini.zkcli;

import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.internal.Lists;

public class CliParameters {
    @Parameter
    public List<String> parameters = Lists.newArrayList();

    @Parameter(names = { "--cluster", "-c" }, description = "Aliased Cluster Name")
    public String cluster;

    @Parameter(names = { "--get", "-g" }, description = "Print data of path* requires a path and cluster")
    public boolean getData;

    @Parameter(names = { "--getChildData" }, description = "Print data of children* requires a path and cluster")
    public boolean getChildData;

    @Parameter(names = { "--getMeta", "-m" }, description = "Whether to print metadata* requires a path and cluster")
    public boolean getMeta;
}
