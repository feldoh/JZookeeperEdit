package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;

import java.util.Set;

public class ZkDeleteHandler {
    /**
     * Delete a node only if it has no children and is not in the given blacklist.
     */
    public void deleteNodeNonRecursive(CuratorFramework client, String path, Set<String> pathBlackList) {
        try {
            if (!pathBlackList.contains(path)) {
                client.delete().forPath(path);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Delete a node including children. Only if it is not in the given blacklist.
     * If a child is blacklisted this method will only delete non-blacklisted children recursively.
     * This will leave a minimum path.
     */
    public void deleteNodeRecursive(CuratorFramework client, String path, Set<String> pathBlackList) {
        try {
            if (!pathBlackList.contains(path)) {
                if (isChildBlacklisted(path, pathBlackList)) {
                    deleteChildrenOfNode(client, path, pathBlackList);
                } else {
                    client.delete().deletingChildrenIfNeeded().forPath(path);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * A convenience method to delete all the children of a node but not the node itself.
     */
    public void deleteChildrenOfNode(CuratorFramework client, String path, Set<String> pathBlackList) {
        try {
            client.getChildren().forPath(path).stream()
                    .map(c -> path.endsWith("/") ? path + c : path + "/" + c)
                    .filter(fullPath -> !pathBlackList.contains(fullPath))
                    .forEach(fullPath -> deleteNodeRecursive(client, fullPath, pathBlackList));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isChildBlacklisted(String path, Set<String> pathBlackList) {
        return pathBlackList.stream().anyMatch(blacklistedPath -> blacklistedPath.startsWith(path));
    }
}
