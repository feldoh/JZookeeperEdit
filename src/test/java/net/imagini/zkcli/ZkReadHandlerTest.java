package net.imagini.zkcli;

import co.unruly.matchers.StreamMatchers;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class ZkReadHandlerTest {

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

    @BeforeEach
    public void setup() {
        underTest = new ZkReadHandler();

        validPathChildren = new ArrayList<>();
        validPathChildren.add("child1");
        validPathChildren.add("child2");
        validPathChildren.add("child3");
    }

    @Test
    public void testGetChildren() throws Exception {
        Mockito.when(client.getChildren()).thenReturn(getChildrenBuilder);
        Mockito.when(getChildrenBuilder.forPath(VALID_PATH)).thenReturn(validPathChildren);
        assertThat(underTest.getChildren(client, VALID_PATH, false),
                StreamMatchers.equalTo(validPathChildren.stream()));
        Mockito.verify(getChildrenBuilder, Mockito.times(1)).forPath(VALID_PATH);
    }

    @Test
    public void testGetChildrenWithPaths() throws Exception {
        Mockito.when(client.getChildren()).thenReturn(getChildrenBuilder);
        Mockito.when(getChildrenBuilder.forPath(VALID_PATH)).thenReturn(validPathChildren);
        assertThat(underTest.getChildren(client, VALID_PATH, true), StreamMatchers.equalTo(validPathChildren.stream()
                                               .map(child -> String.format("%s/%s", VALID_PATH, child))));
        Mockito.verify(getChildrenBuilder, Mockito.times(1)).forPath(VALID_PATH);
    }

    @Test
    public void testGetData() throws Exception {
        Mockito.when(client.getData()).thenReturn(dataBuilder);
        Mockito.when(dataBuilder.forPath(VALID_PATH)).thenReturn(VALID_PATH_DATA);
        assertEquals(new String(VALID_PATH_DATA, CHARSET), underTest.getPathData(client, VALID_PATH));
    }

    @Test
    public void testGetChildrenOfInvalidPath() throws Exception {
        Mockito.when(client.getChildren()).thenReturn(getChildrenBuilder);
        Mockito.doThrow(KeeperException.NoNodeException.class)
                .when(getChildrenBuilder).forPath(Mockito.eq(INVALID_PATH));
        assertThrows(RuntimeException.class, () -> underTest.getChildren(client, INVALID_PATH,false));
        Mockito.verify(getChildrenBuilder, Mockito.times(1)).forPath(INVALID_PATH);
    }

    @Test
    public void testGetDataOfInvalidPath() throws Exception {
        Mockito.when(client.getData()).thenReturn(dataBuilder);
        Mockito.doThrow(KeeperException.NoNodeException.class)
                .when(dataBuilder).forPath(Mockito.eq(INVALID_PATH));
        assertThrows(RuntimeException.class, () -> underTest.getPathData(client, INVALID_PATH));
        Mockito.verify(dataBuilder, Mockito.times(1)).forPath(INVALID_PATH);
    }

}
