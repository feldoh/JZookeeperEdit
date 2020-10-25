package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.*;

public class ZkNodeTest {
    private static final String NODE_LABEL = "someNode";

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Mock
    CuratorFramework mockZkClient;

    private ZkNode unit;

    @Before
    public void setup() {
        Mockito.verifyZeroInteractions(mockZkClient);
    }

    @Test
    public void testLabelIsUsedAsToString() {
        unit = new ZkNode(mockZkClient, NODE_LABEL);
        assertEquals(NODE_LABEL, unit.toString());
    }

    @Test
    public void testUpdatingLabel() {
        unit = new ZkNode(mockZkClient, "initial-val");
        unit.setLabel(NODE_LABEL);
        assertEquals(NODE_LABEL, unit.toString());
    }

    @Test
    public void testNullLabelsAreNotAllowed() {
        var expectedException = assertThrows(RuntimeException.class,
                () -> new ZkNode(mockZkClient, null));
        assertTrue(expectedException.getMessage().contains("label"));
    }

    @Test
    public void testSettingNullLabelsIsNotAllowed() {
        unit = new ZkNode(mockZkClient, "Initial");
        var expectedException = assertThrows(RuntimeException.class,
                () -> unit.setLabel(null));
        assertTrue(expectedException.getMessage().contains("label"));
    }

    @Test
    public void getClient() {
        assertSame(mockZkClient, new ZkNode(mockZkClient, NODE_LABEL).getClient());
    }

    @Test
    public void getNullClient() {
        assertNull(new ZkNode(null, NODE_LABEL).getClient());
    }

    @Test
    public void testIdenticalNodesAreEqual() {
        ZkNode node = new ZkNode(null, NODE_LABEL);
        assertEquals(node, node);
    }

    @Test
    public void testNonNodesAreNotEqual() {
        ZkNode node = new ZkNode(null, NODE_LABEL);
        assertNotEquals(node, new Object());
    }
}