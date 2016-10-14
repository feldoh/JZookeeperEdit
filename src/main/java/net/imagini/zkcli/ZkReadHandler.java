package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;

import java.nio.charset.Charset;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class ZkReadHandler {
    private static final Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;

    /**
     * Get the names of the children stored under a named parent.
     *
     * @param includePaths Whether to prefix the children with their full paths.
     */
    public Stream<String> getChildren(CuratorFramework client, String parentPath, boolean includePaths) {
        try {
            return client.getChildren().forPath(parentPath).stream()
                           .map(String::new)
                           .map(child -> formatNodeName(parentPath, child, includePaths));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the data stored in a node using the default charset.
     */
    public String getPathData(CuratorFramework client, String path) {
        try {
            return new String(client.getData().forPath(path), CHARSET);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String formatNodeName(String path, String child, boolean includePath) {
        StringJoiner joiner = new StringJoiner(path.equals("/") ? "" : "/");
        return (includePath ? joiner.add(path) : joiner)
                       .add(child).toString();
    }

}
