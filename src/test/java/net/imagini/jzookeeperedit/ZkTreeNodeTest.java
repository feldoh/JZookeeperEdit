package net.imagini.jzookeeperedit;

import co.unruly.matchers.StreamMatchers;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.SetDataBuilder;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.zookeeper.data.Stat;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ZkTreeNodeTest {
    private static final Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    private static final String NODE_NAME = "aNode";
    private static final String ROOT_PATH = "/";
    private static final String VALID_PATH = "/valid/path";
    private static final String DATA = "Some very interesting\nDATA!!!!!!!";

    private List<String> validPathChildren;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private CuratorFramework mockClient;
    @Mock
    private GetDataBuilder mockDataBuilder;
    @Mock
    private SetDataBuilder mockSetDataBuilder;
    @Mock
    private ExistsBuilder mockStatBuilder;
    @Mock
    private Stat mockStat;
    @Mock
    private GetChildrenBuilder mockGetChildrenBuilder;

    private ZkTreeNode unit;

    @Before
    public void setup() throws Exception {
        validPathChildren = new ArrayList<>();
        validPathChildren.add("child1");
        validPathChildren.add("child2");
        validPathChildren.add("child3");

        when(mockClient.getState()).thenReturn(CuratorFrameworkState.STARTED);
        when(mockClient.setData()).thenReturn(mockSetDataBuilder);
        when(mockClient.getData()).thenReturn(mockDataBuilder);
        when(mockDataBuilder.forPath(VALID_PATH)).thenReturn(DATA.getBytes(CHARSET));
        when(mockClient.getChildren()).thenReturn(mockGetChildrenBuilder);
        when(mockGetChildrenBuilder.forPath(VALID_PATH)).thenReturn(validPathChildren);
        when(mockClient.checkExists()).thenReturn(mockStatBuilder);
        when(mockStatBuilder.forPath(VALID_PATH)).thenReturn(mockStat);
        when(mockStat.getNumChildren()).thenReturn(validPathChildren.size());
    }

    @Test
    public void testNodesWithoutLoadedChildrenAreNotFiltered() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME, false, VALID_PATH);
        assertFalse(unit.isFiltered());
    }

    @Test
    public void testNodesStartUnfiltered() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME, false, VALID_PATH);
        unit.loadChildren();
        assertFalse(unit.isFiltered());
    }

    @Test
    public void testFilteringSetsIsFiltered() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.loadChildren((x) -> true);
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(validPathChildren.stream()
                        .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        assertTrue(unit.isFiltered());
    }

    @Test
    public void testFilteringLimitsChildren() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.loadChildren((x) -> x.endsWith("2") || x.endsWith("3"));
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(Stream.of("child2", "child3")
                        .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        assertTrue(unit.isFiltered());
    }

    @Test
    public void testGettingChildrenLoadsIfNotLoaded() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(validPathChildren.stream()
                       .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        assertFalse(unit.isFiltered());
    }

    @Test
    public void testGettingChildrenDoesNotLoadIfPreLoaded() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(validPathChildren.stream()
                       .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        assertFalse(unit.isFiltered());
        unit.getChildren();
        Mockito.verify(mockClient, times(1)).getChildren();
    }

    @Test
    public void testToStringUsesFriendlyName() {
        unit = new ZkTreeNode(mockClient, NODE_NAME,true, ROOT_PATH);
        assertEquals(NODE_NAME, unit.getValue().toString());
    }

    @Test
    public void testInvalidateChildrenCacheCausesGetChildrenToReloadOnNextCall() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(validPathChildren.stream()
                       .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        assertFalse(unit.isFiltered());
        unit.getChildren();
        unit.invalidateChildrenCache();
        unit.getChildren();
        Mockito.verify(mockClient, times(2)).getChildren();
    }

    @Test
    public void testReloadingRemovesChildrenFilter() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.loadChildren((x) -> x.endsWith("2") || x.endsWith("3"));
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(Stream.of("child2", "child3")
                       .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        assertTrue(unit.isFiltered());
        unit.invalidateChildrenCache();
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(validPathChildren.stream()
                       .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        assertFalse(unit.isFiltered());
    }

    @Test
    public void invalidateMetadataCache() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        Stat stat = unit.getStat().orElse(null); // Get 1
        assertEquals(stat, mockStat);
        unit.getStat(); // Get 2 (Should reuse)
        unit.getStat(); // Get 3 (Should reuse)
        unit.invalidateMetadataCache();
        unit.getStat(); // Get 4 (Should not reuse)
        Mockito.verify(mockStatBuilder, times(2)).forPath(VALID_PATH);
    }

    @Test
    public void getChildren() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        assertThat(unit.getChildren().stream(),
                StreamMatchers.equalTo(validPathChildren.stream()
                                               .map(name -> new ZkTreeNode(mockClient, name, false, String.join("/", VALID_PATH, name)))));
        Mockito.verify(mockGetChildrenBuilder, times(1)).forPath(VALID_PATH);
    }

    @Test
    public void testIsLeafIsTrueIfNodeHasNoChildren() throws Exception {
        String leafPath = VALID_PATH + "/" + "someLeaf";
        Stat mockLeafStat = Mockito.mock(Stat.class, "leafStat");
        when(mockStatBuilder.forPath(leafPath)).thenReturn(mockLeafStat);
        when(mockLeafStat.getNumChildren()).thenReturn(0);
        assertThat(new ZkTreeNode(mockClient, "leafName",false, leafPath).isLeaf(), is(true));
    }

    @Test
    public void testIsLeafIsFalseIfNodeHasChildren() throws Exception {
        assertFalse(new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH).isLeaf());
    }

    @Test
    public void testCanonicalPathGenerationNormalisesEmptyToSlash() throws Exception {
        assertThat(new ZkTreeNode(mockClient, NODE_NAME, true, "").getCanonicalPath(), equalTo("/"));
        assertThat(new ZkTreeNode(mockClient, NODE_NAME, false, "/some/path").getCanonicalPath(), equalTo("/some/path"));
    }


    @Test
    public void testGetStatUsesCache() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.getStat(); // Should cache
        unit.getStat();
        Mockito.verify(mockStatBuilder, times(1)).forPath(VALID_PATH);
        assertThat(new ZkTreeNode(mockClient, NODE_NAME, false, VALID_PATH).getStat().orElse(null), equalTo(mockStat));
    }

    @Test
    public void testGetStatFromServerIgnoresCache() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.getStatFromServer(true); // Try and make it cache
        unit.getStatFromServer(true); // We should be ignoring the cache and getting anyway
        unit.getStatFromServer(true);
        Mockito.verify(mockStatBuilder, times(3)).forPath(VALID_PATH);
    }

    @Test
    public void getStatFromServerForcesServerCallOnNextRead() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.getStatFromServer(false); // Do not cache this result
        unit.getStat(); // We should be forced to re-get as we did not cache
        Mockito.verify(mockStatBuilder, times(2)).forPath(VALID_PATH);
    }

    @Test
    public void testGetDataReturnsValidContent() throws Exception {
        assertThat(new ZkTreeNode(mockClient, NODE_NAME, false, VALID_PATH).getData().orElse(null), equalTo(DATA));
    }

    @Test
    public void testSaveUsesDefaultEncoding() throws Exception {
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.save(DATA);
        Mockito.verify(mockSetDataBuilder, times(1)).forPath(VALID_PATH, DATA.getBytes(CHARSET));
    }

    @Test
    public void testEnsureActiveOpensLatentConnections() throws Exception {
        when(mockClient.getState()).thenReturn(CuratorFrameworkState.LATENT, CuratorFrameworkState.STARTED);
        unit = new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH);
        unit.getData(); // Should open connection
        unit.getData(); // Should reuse connection
        Mockito.verify(mockClient, times(1)).start();
    }

    @Test
    public void testEqualsCatchesIdentity() {
        assertTrue(new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH)
                           .equals(new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH)));
    }


    @Test
    public void testHashCodeVariesByPath() {
        assertThat(new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH).hashCode(),
                not(new ZkTreeNode(mockClient, NODE_NAME,false, VALID_PATH + "TEST").hashCode()));
    }

}