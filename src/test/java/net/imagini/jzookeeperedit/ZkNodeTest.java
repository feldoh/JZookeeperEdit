package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class ZkNodeTest {
    private static final String NODE_LABEL = "someNode";

    @Mock
    CuratorFramework mockZkClient;

    private ZkNode unit;

    @BeforeEach
    public void setup() {
        Mockito.verifyNoInteractions(mockZkClient);
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