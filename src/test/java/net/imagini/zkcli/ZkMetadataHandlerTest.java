package net.imagini.zkcli;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ZkMetadataHandlerTest {
    @Mock
    private CuratorFramework client;

    @Mock
    private ExistsBuilder statBuilder;

    @Mock
    private Stat mockStat;

    private ZkMetadataHandler underTest;

    private static final String VALID_PATH = "/valid/path";

    private static final String INVALID_PATH = "/invalid/path";
    private static final String STAT_LINE = "someStat\nanother stat line\n";

    private List<String> validPathChildren;

    @BeforeEach
    public void setup() {
        underTest = new ZkMetadataHandler();

        validPathChildren = new ArrayList<>();
        validPathChildren.add("child1");
        validPathChildren.add("child2");
        validPathChildren.add("child3");
    }

    @Test
    public void testGetMeta() throws Exception {
        when(client.checkExists()).thenReturn(statBuilder);
        when(statBuilder.forPath(VALID_PATH)).thenReturn(mockStat);
        when(mockStat.getNumChildren()).thenReturn(validPathChildren.size());
        when(mockStat.toString()).thenReturn(STAT_LINE);
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

    @Test
    public void testGetMetaOfInvalidPath() throws Exception {
        when(client.checkExists()).thenReturn(statBuilder);
        doThrow(KeeperException.NoNodeException.class)
                .when(statBuilder).forPath(eq(INVALID_PATH));
        assertThrows(RuntimeException.class, () -> underTest.getPathMetaData(client, INVALID_PATH,
                "someAccessor"));
        verify(statBuilder, times(1)).forPath(INVALID_PATH);
    }

    @Test
    public void testGetMetaWithInvalidAccessor() throws Exception {
        when(client.checkExists()).thenReturn(statBuilder);
        when(statBuilder.forPath(VALID_PATH)).thenReturn(mockStat);
        assertThrows(RuntimeException.class, () -> underTest.getPathMetaData(client, VALID_PATH,
                "someAccessor"));
        verify(statBuilder, times(1)).forPath(VALID_PATH);
    }
}