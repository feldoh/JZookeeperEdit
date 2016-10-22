package net.imagini.jzookeeperedit;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class ZkClusterManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZkClusterManager.class);
    private static final RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 5, 5000);
    private final Map<String, CuratorFramework> zkClients = new HashMap<>(10);
    private final Properties properties = new Properties();
    private final File workingdir;
    private final File clusterConfigFile;

    /**
     * Loads initial cluster list from a config file stored in the working directory.
     */
    public ZkClusterManager() {
        workingdir = new File(System.getProperty("user.home"), ".JZookeeperEdit");
        if (!workingdir.exists()) {
            if (!workingdir.mkdirs()) {
                LOGGER.error(String.format("Failed to create user config folder: %s",
                        workingdir.getAbsolutePath()));
            }
        }
        clusterConfigFile = new File(workingdir, "clusters.properties");
        if (!clusterConfigFile.exists()) {
            try {
                if (clusterConfigFile.createNewFile()) {
                    LOGGER.info("Created new cluster config file in {}", clusterConfigFile.getAbsolutePath());
                }
            } catch (IOException ex) {
                LOGGER.error(String.format("Failed to create user cluster config file: %s",
                        clusterConfigFile.getAbsolutePath()), ex);
            }
        }
        try (InputStream input = new FileInputStream(clusterConfigFile)) {
            properties.load(input);
            properties.forEach((Object key, Object val) -> addclient((String) key, (String) val));
        } catch (IOException ex) {
            LOGGER.error(String.format("Failed to read user cluster config file: %s",
                    clusterConfigFile.getAbsolutePath()), ex);
        }
    }

    public String getConfigFilePath() {
        return clusterConfigFile.getAbsolutePath();
    }

    /**
     * Create a new client and cache it with a friendly name for later lookup.
     */
    public Optional<CuratorFramework> addclient(String friendlyName, String connectionString) {
        if (friendlyName == null || friendlyName.isEmpty()) {
            throw new IllegalArgumentException("Cannot add a named connection with null name.");
        }
        Optional<CuratorFramework> client = buildClient(connectionString);
        zkClients.put(friendlyName, client.orElse(null));
        properties.put(friendlyName, connectionString);
        return client;
    }

    /**
     * Build a new client from a connection string.
     */
    public Optional<CuratorFramework> buildClient(String connectionString) {
        try {
            return Optional.of(CuratorFrameworkFactory.newClient(connectionString, retryPolicy));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    /**
     * Retrieve an existing client via it's friendly name.
     */
    public Optional<CuratorFramework> getClient(String clusterName) {
        return Optional.ofNullable(zkClients.get(clusterName));
    }

    /**
     * Inverse lookup for finding the friendly name assigned to a client.
     */
    public Optional<String> getFriendlyName(CuratorFramework curatorFramework) {
        return zkClients.entrySet().stream()
                .filter(entry -> curatorFramework.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    /**
     * Save current set of connection details to the user clusters file.
     */
    public void dumpConnectionDetails() throws IOException {
        try (OutputStream output = new FileOutputStream(clusterConfigFile)) {
            properties.store(output, "clusters");
        }
    }
    
    public Map<String, CuratorFramework> getClusters() {
        return Collections.unmodifiableMap(zkClients);
    }
}
