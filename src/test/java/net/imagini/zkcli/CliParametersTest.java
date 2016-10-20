package net.imagini.zkcli;

import com.beust.jcommander.ParameterException;
import net.imagini.jzookeeperedit.ZkClusterManager;
import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class CliParametersTest {

    private CliParameters underTest;

    @Test
    public void testRmrOption() {
        underTest = new CliParameters(new String[]{"--rm-recursive"}, null);
        assertTrue(underTest.isDeleteNodeRecursive());
        underTest = new CliParameters(new String[]{"-r"}, null);
        assertTrue(underTest.isDeleteNodeRecursive());
        underTest = new CliParameters(new String[]{"--rmr"}, null);
        assertTrue(underTest.isDeleteNodeRecursive());
    }

    @Test
    public void testRmOption() {
        underTest = new CliParameters(new String[]{"--rm"}, null);
        assertTrue(underTest.isDeleteNodeNonRecursive());
    }

    @Test
    public void testRmChildrenOption() {
        underTest = new CliParameters(new String[]{"--rm-children"}, null);
        assertTrue(underTest.isDeleteChildrenOfNode());
        underTest = new CliParameters(new String[]{"--rmc"}, null);
        assertTrue(underTest.isDeleteChildrenOfNode());
    }

    @Test
    public void testListOption() {
        underTest = new CliParameters(new String[]{"--ls"}, null);
        assertTrue(underTest.isListChildren());
        underTest = new CliParameters(new String[]{"-l"}, null);
        assertTrue(underTest.isListChildren());
    }

    @Test
    public void testGetDataOption() {
        underTest = new CliParameters(new String[]{"--get"}, null);
        assertTrue(underTest.isGetData());
        underTest = new CliParameters(new String[]{"-g"}, null);
        assertTrue(underTest.isGetData());
    }

    @Test
    public void testGetMetaOption() {
        underTest = new CliParameters(new String[]{"--getMeta"}, null);
        assertTrue(underTest.isGetMeta());
        underTest = new CliParameters(new String[]{"-m"}, null);
        assertTrue(underTest.isGetMeta());
    }

    @Test
    public void testGetMetaAccessorsOption() {
        underTest = new CliParameters(new String[]{"--listMetaFieldAccessors"}, null);
        assertTrue(underTest.isListMetaAccessors());
        underTest = new CliParameters(new String[]{"-a"}, null);
        assertTrue(underTest.isListMetaAccessors());
    }

    @Test
    public void testHelpOption() {
        underTest = new CliParameters(new String[]{"--help"}, null);
        assertTrue(underTest.isHelp());
        underTest = new CliParameters(new String[]{"-h"}, null);
        assertTrue(underTest.isHelp());
        underTest = new CliParameters(new String[]{"-?"}, null);
        assertTrue(underTest.isHelp());
    }

    @Test
    public void testIncludesAction() {
        underTest = new CliParameters(new String[]{"--rmr"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--rm"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--rmc"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--ls"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--get"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--getMeta"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--listMetaFieldAccessors"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--help"}, null);
        assertTrue(underTest.includesAction());
    }

    @Test
    public void testPrintPathsOption() {
        underTest = new CliParameters(new String[]{"--printPaths"}, null);
        assertTrue(underTest.isPrintPaths());
        underTest = new CliParameters(new String[]{"-p"}, null);
        assertTrue(underTest.isPrintPaths());
    }

    @Test
    public void testMetaFieldOption() {
        underTest = new CliParameters(new String[]{"--metaField", "aGetter"}, null);
        assertEquals("aGetter", underTest.getSpecificMetaFieldGetter());
        underTest = new CliParameters(new String[]{"-f", "aGetter"}, null);
        assertEquals("aGetter", underTest.getSpecificMetaFieldGetter());
    }

    @Test
    public void testZkConnectOption() {
        String connectionString = "localhost:2181";
        underTest = new CliParameters(new String[]{"--zkConnect", connectionString}, null);
        assertEquals(connectionString, underTest.getZkConnect());
        underTest = new CliParameters(new String[]{"-z", connectionString}, null);
        assertEquals(connectionString, underTest.getZkConnect());
    }

    @Test
    public void testNamedClusterOption() {
        String friendlyName = "cluster";
        underTest = new CliParameters(new String[]{"--cluster", friendlyName}, null);
        assertEquals(friendlyName, underTest.getFriendlyName());
        underTest = new CliParameters(new String[]{"-c", friendlyName}, null);
        assertEquals(friendlyName, underTest.getFriendlyName());
    }

    @Test
    public void testBlacklistOption() {
        underTest = new CliParameters(new String[]{"--blacklist", "/somePath"}, null);
        assertTrue(underTest.getBlacklist().contains("/somePath"));
        underTest = new CliParameters(new String[]{"--blacklist", "/somePath", "/some/other/path"}, null);
        assertTrue(underTest.getBlacklist().containsAll(Arrays.asList("/somePath", "/some/other/path")));
        underTest = new CliParameters(new String[]{"-b", "/somePath", "-b", "/some/other/path"}, null);
        assertTrue(underTest.getBlacklist().containsAll(Arrays.asList("/somePath", "/some/other/path")));
        underTest = new CliParameters(new String[]{"--preserve", "/somePath", "/some/other/path"}, null);
        assertTrue(underTest.getBlacklist().containsAll(Arrays.asList("/somePath", "/some/other/path")));
        underTest = new CliParameters(new String[]{"--protect", "/somePath", "/some/other/path"}, null);
        assertTrue(underTest.getBlacklist().containsAll(Arrays.asList("/somePath", "/some/other/path")));
    }

    @Test
    public void testDoesNotIncludeAction() {
        underTest = new CliParameters(new String[]{"--printPaths"}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--metaField", ""}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--cluster", "someCluster"}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--zkConnect", "host:1111"}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--blacklist", "/somePath"}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"path/only"}, null);
        assertFalse(underTest.includesAction());
    }

    @Test
    public void testListConstructor() {
        underTest = new CliParameters(Collections.singletonList("-p"), null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(Collections.singletonList("path/only"), null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(Collections.singletonList("-?"), null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(Arrays.asList("--rmc ", "-c", "someCluster"), null);
        assertTrue(underTest.includesAction());
    }

    @Test
    public void testPrintUsage() {
        PrintStream mockStdOut = Mockito.mock(PrintStream.class);
        System.setOut(mockStdOut);
        new CliParameters(new String[]{"-h"}, null).printUsage();
        Mockito.verify(mockStdOut).println(Mockito.contains("JZookeeperEdit [options]"));
    }

    @Test
    public void testNoPositionalParams() {
        underTest = new CliParameters(Arrays.asList("--rmc ", "-c", "someCluster"), null);
        assertEquals(0, underTest.getPositionalParameters().size());
    }

    @Test
    public void testMixOfOptionsAndPositionalParams() {
        underTest = new CliParameters(Arrays.asList("--rmc ", "-c", "someCluster", "/a/path", "/b/path"), null);
        assertEquals(Arrays.asList("/a/path", "/b/path"), underTest.getPositionalParameters());
    }

    @Test
    public void testPositionalParamsOnly() {
        underTest = new CliParameters(Collections.singletonList("/a/path"), null);
        assertEquals(Collections.singletonList("/a/path"), underTest.getPositionalParameters());
    }

    @Test
    public void testGetCluster() {
        String connectionString = "localhost:2181";
        ZkClusterManager mockClusterManager = Mockito.mock(ZkClusterManager.class);
        CuratorFramework mockClient = Mockito.mock(CuratorFramework.class);
        Mockito.when(mockClusterManager.buildClient(connectionString)).thenReturn(Optional.of(mockClient));
        underTest = new CliParameters(Arrays.asList("-z", connectionString), mockClusterManager);
        assertEquals(mockClient, underTest.getCluster().orElseThrow(() -> new IllegalStateException("Client Missing")));
        Mockito.verify(mockClusterManager).buildClient(connectionString);
    }

    @Test
    public void testGetClusterPrioritisesZkConnect() {
        String connectionString = "localhost:2181";
        String friendlyName = "cluster";
        ZkClusterManager mockClusterManager = Mockito.mock(ZkClusterManager.class);
        CuratorFramework mockClient = Mockito.mock(CuratorFramework.class);
        Mockito.when(mockClusterManager.buildClient(connectionString)).thenReturn(Optional.of(mockClient));
        underTest = new CliParameters(Arrays.asList("-c", friendlyName, "-z", connectionString), mockClusterManager);
        assertEquals(mockClient, underTest.getCluster().orElseThrow(() -> new IllegalStateException("Client Missing")));
        Mockito.verify(mockClusterManager).buildClient(connectionString);
        Mockito.verify(mockClusterManager, Mockito.never()).getClient(friendlyName);
    }

    @Test
    public void testGetClusterByName() {
        String friendlyName = "someCluster";
        ZkClusterManager mockClusterManager = Mockito.mock(ZkClusterManager.class);
        CuratorFramework mockClient = Mockito.mock(CuratorFramework.class);
        Mockito.when(mockClusterManager.getClient(friendlyName)).thenReturn(Optional.of(mockClient));
        underTest = new CliParameters(Arrays.asList("-c", friendlyName), mockClusterManager);
        assertEquals(mockClient, underTest.getCluster().orElseThrow(() -> new IllegalStateException("Client Missing")));
        Mockito.verify(mockClusterManager).getClient(friendlyName);
    }

    @Test
    public void testGetClusterWithNoClusterConfigured() {
        ZkClusterManager mockClusterManager = Mockito.mock(ZkClusterManager.class);
        Mockito.verifyNoMoreInteractions(mockClusterManager);
        underTest = new CliParameters(Collections.singletonList("/a/path"), mockClusterManager);
        underTest.getCluster();
        assertFalse(underTest.getCluster().isPresent());
    }

    @Test
    public void testGetClusterHandlesError() {
        String friendlyName = "someCluster";
        ZkClusterManager mockClusterManager = Mockito.mock(ZkClusterManager.class);
        Mockito.when(mockClusterManager.getClient(friendlyName)).thenThrow(new RuntimeException("Fake Error"));
        underTest = new CliParameters(Arrays.asList("-c", friendlyName), mockClusterManager);
        assertFalse(underTest.getCluster().isPresent());
    }

    @Test
    public void combinationIncludesAction() {
        underTest = new CliParameters(new String[] { "--rmc ", "-c", "someCluster" }, null);
        assertTrue(underTest.includesAction());
    }

    @Test(expected = ParameterException.class)
    public void missingParamValue() {
        underTest = new CliParameters(new String[]{"--metaField"}, null);
    }

    @Test
    public void testZkClusterManager() {
        ZkClusterManager manager = Mockito.mock(ZkClusterManager.class);
        underTest = new CliParameters(new String[]{}, manager);
        assertEquals(manager, underTest.getClusterManager());
    }
}
