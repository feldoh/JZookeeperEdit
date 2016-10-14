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
    public void testIncludesAction() {
        underTest = new CliParameters(new String[]{"--rm-recursive"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-r"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"--rmr"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--rm"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--rm-children"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"--rmc"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--ls"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-l"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--get"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-g"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--getMeta"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-m"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--listMetaFieldAccessors"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-a"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--help"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-g"}, null);
        assertTrue(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-?"}, null);
        assertTrue(underTest.includesAction());
    }

    @Test
    public void testDoesNotIncludeAction() {
        underTest = new CliParameters(new String[]{"-p"}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--metaField", ""}, null);
        assertFalse(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-f", ""}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"-c", "someCluster"}, null);
        assertFalse(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-z", "host:1111"}, null);
        assertFalse(underTest.includesAction());
        underTest = new CliParameters(new String[]{"--zkConnect", "host:1111"}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"-b", "/somePath", "/some/other/path"}, null);
        assertFalse(underTest.includesAction());
        underTest = new CliParameters(new String[]{"-b", "/somePath", "-b", "/some/other/path"}, null);
        assertTrue(underTest.blacklist.containsAll(Arrays.asList("/somePath", "/some/other/path")));
        assertFalse(underTest.includesAction());
        underTest = new CliParameters(new String[]{"--blacklist", "/somePath"}, null);
        assertFalse(underTest.includesAction());
        underTest = new CliParameters(new String[]{"--preserve", "/somePath", "/some/other/path"}, null);
        assertTrue(underTest.blacklist.containsAll(Arrays.asList("/somePath", "/some/other/path")));
        assertFalse(underTest.includesAction());
        underTest = new CliParameters(new String[]{"--protect", "/somePath", "/some/other/path"}, null);
        assertTrue(underTest.blacklist.containsAll(Arrays.asList("/somePath", "/some/other/path")));
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

}
