package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

public class ZkMetadataHandler {
    /**
     * Returns the string representation of the 'stat' of the node listed.
     * If an accessor is provided it will instead return only the value of that field.
     *
     * @param metadataItemGetterName the method name of a valid accessor. See {@link #getMetaAccessorMethodNames}
     */
    public String getPathMetaData(CuratorFramework client, String path, String metadataItemGetterName) {
        try {
            return formatMetaData(client.checkExists().forPath(path), metadataItemGetterName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * List the accessors available in {@link Stat}.
     * These should be valid to pass to {@link #getPathMetaData}.
     */
    public Stream<String> getMetaAccessorMethodNames() {
        return Arrays.stream(Stat.class.getDeclaredMethods())
                       .filter(method -> method.getParameterCount() == 0)
                       .map(Method::getName);

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
}
