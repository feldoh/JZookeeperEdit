package net.imagini.jzookeeperedit;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import javax.naming.OperationNotSupportedException;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dlowe
 */
public class ZKClusterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZKClusterManager.class);
    private static final RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5, 5000);
    private static final Map<String, CuratorFramework> zkClients = new HashMap<>(10);
    private static final Properties properties = new Properties();
    private static final File workingdir;
    public static final File clusterConfigFile;
    
    static {
        workingdir = new File(System.getProperty("user.home"), ".JZookeeperEdit");
        if (!workingdir.exists()) {
            workingdir.mkdirs();
        }
        clusterConfigFile = new File(workingdir, "clusters.properties");
        if (!clusterConfigFile.exists()) {
            try {
                clusterConfigFile.createNewFile();
            } catch (IOException ex) {
                LOGGER.error(String.format("Failed to create user cluster config file: %s", clusterConfigFile.getAbsolutePath()), ex);
            }
        }
        InputStream input;
        try {
            input = new FileInputStream(clusterConfigFile);
            properties.load(input);
            properties.forEach((Object key, Object val) -> addclient((String) key, (String) val));
        } catch (IOException ex) {
            LOGGER.error(String.format("Failed to read user cluster config file: %s", clusterConfigFile.getAbsolutePath()), ex);
        }
    }
    
    public static CuratorFramework addclient(String friendlyName, String connectionString) {
        CuratorFramework client = buildClient(connectionString);
        zkClients.put(friendlyName, client);
        properties.put(friendlyName, connectionString);
        return client;
    }

    public static CuratorFramework buildClient(String connectionString) {
        return CuratorFrameworkFactory.newClient(connectionString, retryPolicy);
    }
    
    public static CuratorFramework getClient(String clusterName) {
        return zkClients.get(clusterName);
    }

    public static Optional<String> getFriendlyName(CuratorFramework curatorFramework) {
        return zkClients.entrySet().stream()
                .filter(entry -> curatorFramework.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }
    
    public static void dumpConnectionDetails() throws IOException {
        OutputStream output = new FileOutputStream(clusterConfigFile);
        properties.store(output, "clusters");
    }
    
    public static Map<String, CuratorFramework> getClusters(){
        return Collections.unmodifiableMap(zkClients);
    }
    
    private ZKClusterManager() throws OperationNotSupportedException {
        throw new OperationNotSupportedException("All methods of this class are static");
    }
}
