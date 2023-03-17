package projekt.delivery.routing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import projekt.ComparableUnitTests;
import projekt.ObjectUnitTests;
import projekt.base.Location;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import java.util.function.Function;

public class NodeImplUnitTests {

    private static ComparableUnitTests<NodeImpl> comparableUnitTests;
    private static ObjectUnitTests<NodeImpl> objectUnitTests;
    private static NodeImpl nodeA;
    private static NodeImpl nodeB;
    private static NodeImpl nodeC;
    private static NodeImpl nodeD;

    private static EdgeImpl edgeAA;
    private static EdgeImpl edgeAB;
    private static EdgeImpl edgeBC;

    @BeforeAll
    public static void initialize() {
        RegionImpl region1 = new RegionImpl();

        Function<Integer, NodeImpl> testObjectFactory = (i) -> new NodeImpl(new RegionImpl(), "Test",
                new Location(i, i * i), null);
        Function<NodeImpl, String> testStringFactory = (o) -> o.toString();
        objectUnitTests = new ObjectUnitTests<>(testObjectFactory, testStringFactory);
        objectUnitTests.initialize(10);
        comparableUnitTests = new ComparableUnitTests<>(testObjectFactory);
        comparableUnitTests.initialize(10);

        Location locationA = new Location(0, 0);
        Location locationB = new Location(0, 1);
        Location locationC = new Location(1, 0);
        Location locationD = new Location(2, 0);

        edgeAA = new EdgeImpl(region1, "AA", locationA, locationA, 0);
        edgeAB = new EdgeImpl(region1, "AB", locationA, locationB, 0);
        edgeBC = new EdgeImpl(region1, "BC", locationB, locationC, 0);

        nodeA = new NodeImpl(region1, "A", locationA, Set.of(locationA, locationB));
        nodeB = new NodeImpl(region1, "B", locationB, Set.of(locationA, locationC));
        nodeC = new NodeImpl(region1, "C", locationC, Set.of(locationB));
        nodeD = new NodeImpl(region1, "D", locationD, Set.of());

        region1.putNode(nodeA);
        region1.putNode(nodeB);
        region1.putNode(nodeC);
        region1.putNode(nodeD);
        region1.putEdge(edgeAA);
        region1.putEdge(edgeAB);
        region1.putEdge(edgeBC);
    }

    @Test
    public void testEquals() {
        objectUnitTests.testEquals();
    }

    @Test
    public void testHashCode() {
        objectUnitTests.testHashCode();
    }

    @Test
    public void testToString() {
        objectUnitTests.testToString();
    }

    @Test
    public void testBiggerThen() {
        comparableUnitTests.testBiggerThen();
    }

    @Test
    public void testAsBigAs() {
        comparableUnitTests.testAsBigAs();
    }

    @Test
    public void testLessThen() {
        comparableUnitTests.testLessThen();
    }

    @Test
    public void testGetEdge() {
        assertEquals(nodeA.getEdge(nodeA), edgeAA);
        assertEquals(nodeA.getEdge(nodeB), edgeAB);
        assertNull(nodeA.getEdge(nodeC));
        assertNull(nodeA.getEdge(nodeD));
    }

    @Test
    public void testAdjacentNodes() {
        assertEquals(nodeA.getAdjacentEdges(), Set.of(edgeAA, edgeAB));
        assertEquals(nodeB.getAdjacentEdges(), Set.of(edgeAB, edgeBC));
        assertEquals(nodeC.getAdjacentEdges(), Set.of(edgeBC));
        assertEquals(nodeD.getAdjacentEdges(), Set.of());
    }

    @Test
    public void testAdjacentEdges() {
        assertEquals(nodeA.getAdjacentNodes(), Set.of(nodeA, nodeB));
        assertEquals(nodeB.getAdjacentNodes(), Set.of(nodeA, nodeC));
        assertEquals(nodeC.getAdjacentNodes(), Set.of(nodeB));
        assertEquals(nodeD.getAdjacentNodes(), Set.of());
    }
}
