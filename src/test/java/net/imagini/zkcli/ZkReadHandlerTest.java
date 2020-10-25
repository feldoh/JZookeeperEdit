package net.imagini.zkcli;

import co.unruly.matchers.StreamMatchers;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.ExistsBuilder;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.zookeeper.KeeperException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

public class ZkReadHandlerTest {
    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    private CuratorFramework client;

    @Mock
    private GetDataBuilder dataBuilder;
    @Mock
    private GetChildrenBuilder getChildrenBuilder;

    private ZkReadHandler underTest;

    private static final Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;
    private static final String VALID_PATH = "/valid/path";
    private static final byte[] VALID_PATH_DATA = "Some data".getBytes();

    private static final String INVALID_PATH = "/invalid/path";

    private List<String> validPathChildren;

    @Before
    public void setup() throws Exception {
        underTest = new ZkReadHandler();

        validPathChildren = new ArrayList<>();
        validPathChildren.add("child1");
        validPathChildren.add("child2");
        validPathChildren.add("child3");

        Mockito.when(client.getData()).thenReturn(dataBuilder);
        Mockito.when(dataBuilder.forPath(VALID_PATH)).thenReturn(VALID_PATH_DATA);
        Mockito.when(client.getChildren()).thenReturn(getChildrenBuilder);
        Mockito.when(getChildrenBuilder.forPath(VALID_PATH)).thenReturn(validPathChildren);
        Mockito.doThrow(KeeperException.NoNodeException.class)
                .when(getChildrenBuilder).forPath(Mockito.eq(INVALID_PATH));
        Mockito.doThrow(KeeperException.NoNodeException.class)
                .when(dataBuilder).forPath(Mockito.eq(INVALID_PATH));
    }

    @Test
    public void testGetChildren() throws Exception {
        assertThat(underTest.getChildren(client, VALID_PATH, false),
                StreamMatchers.equalTo(validPathChildren.stream()));
        Mockito.verify(getChildrenBuilder, Mockito.times(1)).forPath(VALID_PATH);
    }

    @Test
    public void testGetChildrenWithPaths() throws Exception {
        assertThat(underTest.getChildren(client, VALID_PATH, true), StreamMatchers.equalTo(validPathChildren.stream()
                                               .map(child -> String.format("%s/%s", VALID_PATH, child))));
        Mockito.verify(getChildrenBuilder, Mockito.times(1)).forPath(VALID_PATH);
    }

    @Test
    public void testGetData() {
        assertEquals(new String(VALID_PATH_DATA, CHARSET), underTest.getPathData(client, VALID_PATH));
    }

    @Test(expected = RuntimeException.class)
    public void testGetChildrenOfInvalidPath() throws Exception {
        try {
            underTest.getChildren(client, INVALID_PATH, false);
        } finally {
            Mockito.verify(getChildrenBuilder, Mockito.times(1)).forPath(INVALID_PATH);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testGetDataOfInvalidPath() throws Exception {
        try {
            underTest.getPathData(client, INVALID_PATH);
        } finally {
            Mockito.verify(dataBuilder, Mockito.times(1)).forPath(INVALID_PATH);
        }
    }

}
