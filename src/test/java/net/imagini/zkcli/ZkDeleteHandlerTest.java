package net.imagini.zkcli;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.BackgroundVersionable;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ZkDeleteHandlerTest {

    @Mock
    private CuratorFramework client;

    @Mock
    private BackgroundVersionable backgroundVersionable;

    @Mock
    private DeleteBuilder deleteBuilder;

    @Mock
    private GetChildrenBuilder getChildrenBuilder;

    private ZkDeleteHandler underTest;

    private static final String VALID_PATH = "/valid/path";

    private static final String INVALID_PATH = "/invalid/path";

    private static final String protectedZkMetaNode = "/zookeeper";

    private List<String> validPathChildren;

    @Before
    public void setup() throws Exception {
        underTest = new ZkDeleteHandler();

        validPathChildren = new ArrayList<>();
        validPathChildren.add("child1");
        validPathChildren.add("child2");
        validPathChildren.add("child3");

        when(client.delete()).thenReturn(deleteBuilder);
        when(client.getChildren()).thenReturn(getChildrenBuilder);
        when(getChildrenBuilder.forPath(VALID_PATH)).thenReturn(validPathChildren);
        when(deleteBuilder.deletingChildrenIfNeeded()).thenReturn(backgroundVersionable);
        doThrow(KeeperException.NoNodeException.class)
                .when(backgroundVersionable).forPath(eq(INVALID_PATH));
        doThrow(KeeperException.NoNodeException.class)
                .when(deleteBuilder).forPath(eq(INVALID_PATH));
    }

    @Test
    public void testDeleteNodeNonRecursiveNormal() throws Exception {
        underTest.deleteNodeNonRecursive(client, VALID_PATH, Collections.singleton(protectedZkMetaNode));

        verify(deleteBuilder, times(1)).forPath(VALID_PATH);
        verifyNoMoreInteractions(deleteBuilder);
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteNodeNonRecursiveBadPath() {
        underTest.deleteNodeNonRecursive(client, INVALID_PATH, Collections.singleton(protectedZkMetaNode));
    }

    @Test
    public void testDeleteNodeRecursiveNormal() throws Exception {
        underTest.deleteNodeRecursive(client, VALID_PATH, Collections.singleton(protectedZkMetaNode));

        verify(deleteBuilder, times(1)).deletingChildrenIfNeeded();
        verify(backgroundVersionable, times(1)).forPath(VALID_PATH);
    }

    @Test
    public void testDeleteChildrenNormal() throws Exception {
        underTest.deleteChildrenOfNode(client, VALID_PATH, Collections.singleton(protectedZkMetaNode));

        verify(deleteBuilder, times(validPathChildren.size())).deletingChildrenIfNeeded();
        verify(backgroundVersionable, times(validPathChildren.size())).forPath(anyString());
        verify(backgroundVersionable, times(0)).forPath(VALID_PATH);
    }

    @Test
    public void testDeleteChildrenOfRoot() throws Exception {
        validPathChildren.add("zookeeper");
        when(getChildrenBuilder.forPath("/")).thenReturn(validPathChildren);

        underTest.deleteChildrenOfNode(client, "/", Collections.singleton(protectedZkMetaNode));

        verify(deleteBuilder, times(validPathChildren.size() - 1)).deletingChildrenIfNeeded();
        verify(backgroundVersionable, times(validPathChildren.size() - 1)).forPath(anyString());
        verify(backgroundVersionable, times(0)).forPath("/zookeeper");
    }

    @Test
    public void testDeleteChildrenWithNonRootZookeeperInPath() throws Exception {
        validPathChildren.add("zookeeper");

        underTest.deleteChildrenOfNode(client, VALID_PATH, Collections.singleton(protectedZkMetaNode));

        verify(deleteBuilder, times(validPathChildren.size())).deletingChildrenIfNeeded();
        verify(backgroundVersionable, times(validPathChildren.size())).forPath(anyString());
    }

    @Test
    public void testDeleteNodeRecursiveWithBlacklistedChildren() throws Exception {
        String protectedChild = "protected";
        String protectedChildPath = VALID_PATH + "/" + protectedChild;
        validPathChildren.add(protectedChild);
        underTest.deleteNodeRecursive(client, VALID_PATH, new HashSet<String>(){{
            add(protectedZkMetaNode);
            add(protectedChildPath);
        }});

        verify(deleteBuilder, never()).forPath(protectedChildPath);
        verify(deleteBuilder, times(validPathChildren.size() - 1)).deletingChildrenIfNeeded();
        verify(backgroundVersionable, times(validPathChildren.size() - 1)).forPath(anyString());
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteNodeChildrenBadPath() throws Exception {
        underTest.deleteChildrenOfNode(client, INVALID_PATH, Collections.singleton(protectedZkMetaNode));
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteNodeRecursiveBadPath() throws Exception {
        underTest.deleteNodeRecursive(client, INVALID_PATH, Collections.singleton(protectedZkMetaNode));
    }
}
