package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ZkCliTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private CliParameters mockParams;
    @Mock
    private ZkDeleteHandler mockDeleteHandler;
    @Mock
    private ZkReadHandler mockReadHandler;
    @Mock
    private ZkMetadataHandler mockMetaHandler;
    @Mock
    private CuratorFramework mockClient;
    @Mock
    private PrintStream mockStdOut;

    @Before
    public void setup() {
        System.setOut(mockStdOut);
    }

    @Test
    public void testPrintingHelp() {
        when(mockParams.isHelp()).thenReturn(true);
        new ZkCli(mockParams, null, null, null).run();
        verify(mockParams).printUsage();
    }

    @Test
    public void testPrintingAccessors() {
        when(mockParams.isListMetaAccessors()).thenReturn(true);
        new ZkCli(mockParams, null, null, mockMetaHandler).run();
        verify(mockMetaHandler).getMetaAccessorMethodNames();
    }

    @Test
    public void testNoClusterProvidedThrowsException() {
        when(mockParams.getCluster()).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> new ZkCli(mockParams, null, null, null)
                        .run());
    }

    @Test
    public void testCannotConnectThrowsException() throws InterruptedException {
        when(mockParams.getFriendlyName()).thenReturn("someCluster");
        when(mockParams.getCluster()).thenReturn(Optional.of(mockClient));
        when(mockClient.blockUntilConnected(anyInt(), any(TimeUnit.class))).thenReturn(false);
        var expectedException = assertThrows(IllegalStateException.class,
                () -> new ZkCli(mockParams, null, null, null)
                        .run());
        assertTrue(expectedException.getMessage().contains("someCluster"));
    }

    @Test
    public void testConnectionInterruptedRetries() throws InterruptedException {
        when(mockParams.getCluster()).thenReturn(Optional.of(mockClient));
        when(mockClient.blockUntilConnected(anyInt(), any(TimeUnit.class)))
                .thenThrow(InterruptedException.class)
                .thenReturn(true);
        new ZkCli(mockParams, null, null, null).run();
        verify(mockClient).close();
    }

    @Test
    public void failingToCloseIsNotAnError() throws InterruptedException {
        when(mockParams.getCluster()).thenReturn(Optional.of(mockClient));
        doThrow(new RuntimeException()).when(mockClient).close();
        when(mockClient.blockUntilConnected(anyInt(), any(TimeUnit.class))).thenReturn(true);
        new ZkCli(mockParams, null, null, null).run();
        verify(mockClient).close();
    }

    @Test
    public void testDeleteChildrenHappensBeforeParent() throws InterruptedException {
        String path = "/a/path";
        when(mockParams.getCluster()).thenReturn(Optional.of(mockClient));
        when(mockParams.getPositionalParameters()).thenReturn(Collections.singletonList(path));
        when(mockParams.isDeleteChildrenOfNode()).thenReturn(true);
        when(mockParams.isDeleteNodeNonRecursive()).thenReturn(true);
        doThrow(new RuntimeException()).when(mockClient).close();
        when(mockClient.blockUntilConnected(anyInt(), any(TimeUnit.class))).thenReturn(true);
        new ZkCli(mockParams, mockDeleteHandler, null, null).run();
        InOrder inOrder = inOrder(mockDeleteHandler, mockClient);
        inOrder.verify(mockDeleteHandler).deleteChildrenOfNode(eq(mockClient), eq(path), anySet());
        inOrder.verify(mockDeleteHandler).deleteNodeNonRecursive(eq(mockClient), eq(path), anySet());
        inOrder.verify(mockClient).close();
    }

    @Test
    public void testMixingPrintOptionsClustersAnswersByPath() throws InterruptedException {
        String pathA = "/a/path";
        String dataA = "aData";
        String metaA = "aMeta";
        String pathB = "/b/path";
        String dataB = "bData";
        String metaB = "bMeta";
        String metaGetter = "someGetter";
        List<String> paths = Arrays.asList(pathA, pathB);
        when(mockParams.getCluster()).thenReturn(Optional.of(mockClient));
        when(mockParams.getPositionalParameters()).thenReturn(paths);
        when(mockParams.isGetData()).thenReturn(true);
        when(mockParams.isGetMeta()).thenReturn(true);
        when(mockParams.getSpecificMetaFieldGetter()).thenReturn(metaGetter);
        when(mockReadHandler.getPathData(mockClient, pathA)).thenReturn(dataA);
        when(mockReadHandler.getPathData(mockClient, pathB)).thenReturn(dataB);
        when(mockMetaHandler.getPathMetaData(mockClient, pathA, metaGetter)).thenReturn(metaA);
        when(mockMetaHandler.getPathMetaData(mockClient, pathB, metaGetter)).thenReturn(metaB);
        doThrow(new RuntimeException()).when(mockClient).close();
        when(mockClient.blockUntilConnected(anyInt(), any(TimeUnit.class))).thenReturn(true);
        new ZkCli(mockParams, null, mockReadHandler, mockMetaHandler).run();
        InOrder inOrder = inOrder(mockStdOut, mockClient);
        inOrder.verify(mockStdOut).println(dataA);
        inOrder.verify(mockStdOut).println(metaA);
        inOrder.verify(mockStdOut).println(dataB);
        inOrder.verify(mockStdOut).println(metaB);
        inOrder.verify(mockClient).close();
    }

    @Test
    public void testOneArgConstructor() throws NoSuchFieldException, IllegalAccessException {
        ZkCli unit = new ZkCli(mockParams);
        Field zkDeleteHandler = ZkCli.class.getDeclaredField("zkDeleteHandler");
        zkDeleteHandler.setAccessible(true);
        assertNotNull(zkDeleteHandler.get(unit));
        Field zkReadHandler = ZkCli.class.getDeclaredField("zkReadHandler");
        zkReadHandler.setAccessible(true);
        assertNotNull(zkReadHandler.get(unit));
        Field zkMetadataHandler = ZkCli.class.getDeclaredField("zkMetadataHandler");
        zkMetadataHandler.setAccessible(true);
        assertNotNull(zkMetadataHandler.get(unit));
    }

    @Test
    public void testListingHappensBeforeDeleting() throws InterruptedException {
        String path = "/a/path";
        String childA = "a";
        String childB = "b";
        Stream<String> children = Stream.of(childA, childB);
        when(mockParams.getCluster()).thenReturn(Optional.of(mockClient));
        when(mockParams.getPositionalParameters()).thenReturn(Collections.singletonList(path));
        when(mockParams.isListChildren()).thenReturn(true);
        when(mockParams.isPrintPaths()).thenReturn(false);
        when(mockParams.isDeleteNodeRecursive()).thenReturn(true);
        when(mockClient.blockUntilConnected(anyInt(), any(TimeUnit.class))).thenReturn(true);
        when(mockReadHandler.getChildren(mockClient, path, false)).thenReturn(children);
        new ZkCli(mockParams, mockDeleteHandler, mockReadHandler, null).run();
        InOrder inOrder = inOrder(mockReadHandler, mockDeleteHandler, mockClient, mockStdOut);
        inOrder.verify(mockReadHandler).getChildren(mockClient, path, false);
        inOrder.verify(mockStdOut).println(childA);
        inOrder.verify(mockStdOut).println(childB);
        inOrder.verify(mockDeleteHandler).deleteNodeRecursive(eq(mockClient), eq(path), anySet());
        inOrder.verify(mockClient).close();
    }
}