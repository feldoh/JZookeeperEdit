package net.imagini.zkcli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import net.imagini.jzookeeperedit.ZkClusterManager;

import com.beust.jcommander.ParameterException;

@RunWith(MockitoJUnitRunner.class)
public class CliParametersTest {
    @Mock
    private CuratorFramework mockClient;
    @Mock
    private ZkClusterManager mockClusterManager;

    private CliParameters underTest;

    @Test
    public void testIncludesAction() {
        underTest = new CliParameters(new String[]{"--rmr"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--listMetaFieldAccessors"}, null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--help"}, null);
        assertTrue(underTest.includesAction());
    }

    @Test
    public void testBlacklistOption() {
        underTest = new CliParameters(new String[]{"--protect", "/somePath"}, null);
        assertTrue(underTest.getBlacklist().contains("/somePath"));
        underTest = new CliParameters(new String[]{"--blacklist", "/somePath", "/some/other/path"}, null);
        assertTrue(underTest.getBlacklist().containsAll(Arrays.asList("/somePath", "/some/other/path")));
        underTest = new CliParameters(new String[]{"-b", "/somePath", "-b", "/some/other/path"}, null);
        assertTrue(underTest.getBlacklist().containsAll(Arrays.asList("/somePath", "/some/other/path")));
    }

    @Test
    public void testDoesNotIncludeAction() {
        underTest = new CliParameters(new String[]{"--printPaths"}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"--metaField", ""}, null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(new String[]{"path/only"}, null);
        assertFalse(underTest.includesAction());
    }

    @Test
    public void testListConstructor() {
        underTest = new CliParameters(Collections.singletonList("-p"), null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(Collections.singletonList("/path/only"), null);
        assertFalse(underTest.includesAction());

        underTest = new CliParameters(Collections.singletonList("-?"), null);
        assertTrue(underTest.includesAction());

        underTest = new CliParameters(Arrays.asList("--rmc ", "-c", "someCluster"), null);
        assertTrue(underTest.includesAction());
    }

    @Test
    public void testPrintUsage() {
        PrintStream mockStdOut = mock(PrintStream.class);
        System.setOut(mockStdOut);
        new CliParameters(new String[]{"-h"}, null).printUsage();
        verify(mockStdOut).println(contains("JZookeeperEdit [options]"));
    }

    @Test
    public void testNoPositionalParams() {
        underTest = new CliParameters(Arrays.asList("--rmc ", "-c", "someCluster"), null);
        assertEquals(0, underTest.getPositionalParameters().size());
    }

    @Test
    public void testMixOfOptionsAndPositionalParams() {
        underTest = new CliParameters(Arrays.asList("--rmc", "--rm", "-c", "someCluster", "/a/path", "/b/path"), null);
        assertEquals(Arrays.asList("/a/path", "/b/path"), underTest.getPositionalParameters());

        assertEquals("someCluster", underTest.getFriendlyName());
        assertEquals(null, underTest.getSpecificMetaFieldGetter());
        assertEquals(null, underTest.getZkConnect());

        assertTrue(underTest.isDeleteNodeNonRecursive());
        assertTrue(underTest.isDeleteChildrenOfNode());

        assertFalse(underTest.isDeleteNodeRecursive());
        assertFalse(underTest.isListMetaAccessors());
        assertFalse(underTest.isListChildren());
        assertFalse(underTest.isPrintPaths());
        assertFalse(underTest.isGetData());
        assertFalse(underTest.isGetMeta());
        assertFalse(underTest.isHelp());
    }

    @Test
    public void testPositionalParamsOnly() {
        underTest = new CliParameters(Collections.singletonList("/a/path"), null);
        assertEquals(Collections.singletonList("/a/path"), underTest.getPositionalParameters());
    }

    @Test
    public void testGetCluster() {
        String connectionString = "localhost:2181";
        when(mockClusterManager.buildClient(connectionString)).thenReturn(Optional.of(mockClient));
        underTest = new CliParameters(Arrays.asList("-z", connectionString), mockClusterManager);
        assertEquals(mockClient, underTest.getCluster().orElseThrow(() -> new IllegalStateException("Client Missing")));
        verify(mockClusterManager).buildClient(connectionString);
    }

    @Test
    public void testGetClusterPrioritisesZkConnect() {
        String connectionString = "localhost:2181";
        String friendlyName = "cluster";
        when(mockClusterManager.buildClient(connectionString)).thenReturn(Optional.of(mockClient));
        underTest = new CliParameters(Arrays.asList("-c", friendlyName, "-z", connectionString), mockClusterManager);
        assertEquals(mockClient, underTest.getCluster().orElseThrow(() -> new IllegalStateException("Client Missing")));
        verify(mockClusterManager).buildClient(connectionString);
        verify(mockClusterManager, never()).getClient(friendlyName);
    }

    @Test
    public void testGetClusterByName() {
        String friendlyName = "someCluster";
        when(mockClusterManager.getClient(friendlyName)).thenReturn(Optional.of(mockClient));
        underTest = new CliParameters(Arrays.asList("-c", friendlyName), mockClusterManager);
        assertEquals(mockClient, underTest.getCluster().orElseThrow(() -> new IllegalStateException("Client Missing")));
        verify(mockClusterManager).getClient(friendlyName);
    }

    @Test
    public void testGetClusterWithNoClusterConfigured() {
        verifyZeroInteractions(mockClusterManager);
        underTest = new CliParameters(Collections.singletonList("/a/path"), mockClusterManager);
        underTest.getCluster();
        assertFalse(underTest.getCluster().isPresent());
    }

    @Test
    public void testGetClusterHandlesError() {
        String friendlyName = "someCluster";
        when(mockClusterManager.getClient(friendlyName)).thenThrow(new RuntimeException("Fake Error"));
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
        underTest = new CliParameters(new String[]{}, mockClusterManager);
        assertEquals(mockClusterManager, underTest.getClusterManager());
    }
}
