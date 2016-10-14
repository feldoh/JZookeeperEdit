package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ZkMetadataHandlerTest {
    @Mock
    private CuratorFramework client;

    @Mock
    private GetDataBuilder dataBuilder;
    @Mock
    private ExistsBuilder statBuilder;
    @Mock
    private GetChildrenBuilder getChildrenBuilder;

    @Mock
    private Stat mockStat;

    private ZkMetadataHandler underTest;

    private static final String VALID_PATH = "/valid/path";

    private static final String INVALID_PATH = "/invalid/path";
    private static final String STAT_LINE = "someStat\nanother stat line\n";

    private List<String> validPathChildren;

    @Before
    public void setup() throws Exception {
        underTest = new ZkMetadataHandler();

        validPathChildren = new ArrayList<>();
        validPathChildren.add("child1");
        validPathChildren.add("child2");
        validPathChildren.add("child3");

        Mockito.when(client.checkExists()).thenReturn(statBuilder);
        Mockito.when(statBuilder.forPath(VALID_PATH)).thenReturn(mockStat);
        Mockito.when(mockStat.getNumChildren()).thenReturn(validPathChildren.size());
        Mockito.when(mockStat.toString()).thenReturn(STAT_LINE);
        Mockito.doThrow(KeeperException.NoNodeException.class)
                .when(statBuilder).forPath(Mockito.eq(INVALID_PATH));
    }

    @Test
    public void testGetMeta() {
        assertEquals(STAT_LINE, underTest.getPathMetaData(client, VALID_PATH, null));
        assertEquals(STAT_LINE, underTest.getPathMetaData(client, VALID_PATH, ""));

        assertEquals(String.valueOf(validPathChildren.size()),
                underTest.getPathMetaData(client, VALID_PATH, "getNumChildren"));
    }

    @Test
    public void testListingAccessors() {
        assertTrue(underTest.getMetaAccessorMethodNames().anyMatch("getNumChildren"::equals));
        assertTrue(underTest.getMetaAccessorMethodNames().noneMatch("serialize"::equals));
    }

    @Test(expected = RuntimeException.class)
    public void testGetMetaOfInvalidPath() throws Exception {
        try {
            underTest.getPathMetaData(client, INVALID_PATH, "someAccessor");
        } finally {
            Mockito.verify(statBuilder, Mockito.times(1)).forPath(INVALID_PATH);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testGetMetaWithInvalidAccessor() throws Exception {
        try {
            underTest.getPathMetaData(client, VALID_PATH, "someAccessor");
        } finally {
            Mockito.verify(statBuilder, Mockito.times(1)).forPath(VALID_PATH);
        }
    }
}