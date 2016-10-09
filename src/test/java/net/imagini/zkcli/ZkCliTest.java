package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundVersionable;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ZkCliTest {

    @Mock
    private CuratorFramework client;

    @Mock
    private BackgroundVersionable backgroundVersionable;

    @Mock
    private DeleteBuilder deleteBuilder;

    @Mock
    private GetChildrenBuilder getChildrenBuilder;

    @Mock
    private CliParameters cliParameters;

    private ZkCli underTest;

    private static final String VALID_PATH = "/valid/path";

    private static final String INVALID_PATH = "/invalid/path";

    private List<String> validPathChildren;

    @Before
    public void setup() throws Exception {
        underTest = new ZkCli(cliParameters);

        validPathChildren = new ArrayList<>();
        validPathChildren.add("child1");
        validPathChildren.add("child2");
        validPathChildren.add("child3");

        Mockito.when(client.delete()).thenReturn(deleteBuilder);
        Mockito.when(client.getChildren()).thenReturn(getChildrenBuilder);
        Mockito.when(getChildrenBuilder.forPath(VALID_PATH)).thenReturn(validPathChildren);
        Mockito.when(deleteBuilder.deletingChildrenIfNeeded()).thenReturn(backgroundVersionable);
        Mockito.doThrow(KeeperException.NoNodeException.class)
                .when(backgroundVersionable).forPath(Mockito.eq(INVALID_PATH));
        Mockito.doThrow(KeeperException.NoNodeException.class)
                .when(deleteBuilder).forPath(Mockito.eq(INVALID_PATH));
    }

    @Test
    public void testDeleteNodeNonRecursiveNormal() throws Exception {
        underTest.deleteNodeNonRecursive(client, VALID_PATH);

        Mockito.verify(deleteBuilder, Mockito.times(1)).forPath(VALID_PATH);
        Mockito.verifyNoMoreInteractions(deleteBuilder);
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteNodeNonRecursiveBadPath() {
        underTest.deleteNodeNonRecursive(client, INVALID_PATH);
    }

    @Test
    public void testDeleteNodeRecursiveNormal() throws Exception {
        underTest.deleteNodeRecursive(client, VALID_PATH);

        Mockito.verify(deleteBuilder, Mockito.times(1)).deletingChildrenIfNeeded();
        Mockito.verify(backgroundVersionable, Mockito.times(1)).forPath(VALID_PATH);
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteNodeRecursiveBadPath() throws Exception {
        underTest.deleteNodeRecursive(client, INVALID_PATH);
    }

    @Test
    public void testDeleteChildrenNormal() throws Exception {
        underTest.deleteChildrenOfNode(client, VALID_PATH);

        Mockito.verify(deleteBuilder, Mockito.times(validPathChildren.size())).deletingChildrenIfNeeded();
        Mockito.verify(backgroundVersionable, Mockito.times(validPathChildren.size())).forPath(Mockito.anyString());
        Mockito.verify(backgroundVersionable, Mockito.times(0)).forPath(VALID_PATH);
    }

    @Test
    public void testDeleteChildrenOfRoot() throws Exception {
        validPathChildren.add("zookeeper");
        Mockito.when(getChildrenBuilder.forPath("/")).thenReturn(validPathChildren);

        underTest.deleteChildrenOfNode(client, "/");

        Mockito.verify(deleteBuilder, Mockito.times(validPathChildren.size() - 1)).deletingChildrenIfNeeded();
        Mockito.verify(backgroundVersionable, Mockito.times(validPathChildren.size() - 1)).forPath(Mockito.anyString());
        Mockito.verify(backgroundVersionable, Mockito.times(0)).forPath("/zookeeper");
    }

    @Test
    public void testDeleteChildrenWithNonRootZookeeperInPath() throws Exception {
        validPathChildren.add("zookeeper");

        underTest.deleteChildrenOfNode(client, VALID_PATH);

        Mockito.verify(deleteBuilder, Mockito.times(validPathChildren.size())).deletingChildrenIfNeeded();
        Mockito.verify(backgroundVersionable, Mockito.times(validPathChildren.size())).forPath(Mockito.anyString());
    }
}