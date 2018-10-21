package net.imagini.jzookeeperedit;

import org.apache.curator.framework.CuratorFramework;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.*;

public class ZkNodeTest {
    private static final String NODE_LABEL = "someNode";

    @Rule
    public MockitoRule mockito = MockitoJUnit.rule();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("label");
        unit = new ZkNode(mockZkClient, null);
    }

    @Test
    public void testSettingNullLabelsIsNotAllowed() {
        unit = new ZkNode(mockZkClient, "Initial");
        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("label");
        unit.setLabel(null);
    }

    @Test
    public void getClient() throws Exception {
        assertSame(mockZkClient, new ZkNode(mockZkClient, NODE_LABEL).getClient());
    }

    @Test
    public void getNullClient() throws Exception {
        assertNull(new ZkNode(null, NODE_LABEL).getClient());
    }

    @Test
    public void testIdenticalNodesAreEqual() {
        ZkNode node = new ZkNode(null, NODE_LABEL);
        assertTrue(node.equals(node));
    }

    @Test
    public void testNonNodesAreNotEqual() {
        ZkNode node = new ZkNode(null, NODE_LABEL);
        assertFalse(node.equals(new Object()));
    }
}