package projekt.delivery.routing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import projekt.ComparableUnitTests;
import projekt.ObjectUnitTests;
import projekt.base.Location;

import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class EdgeImplUnitTests {

    private static ComparableUnitTests<Region.Edge> comparableUnitTests;
    private static ObjectUnitTests<Region.Edge> objectUnitTests;
    private static NodeImpl nodeA;
    private static NodeImpl nodeB;
    private static NodeImpl nodeC;

    private static EdgeImpl edgeAA;
    private static EdgeImpl edgeAB;
    private static EdgeImpl edgeBC;

    @BeforeAll
    public static void initialize() {
        RegionImpl region1 = new RegionImpl();

        Function<Integer, Region.Edge> testObjectFactory = (i) -> {
            RegionImpl region2 = new RegionImpl();
            Location locationA = new Location(i - 1, i * i);
            Location locationB = new Location(i, i * i);
            NodeImpl nodeA = new NodeImpl(region2, "A", locationA, Set.of(locationB));
            NodeImpl nodeB = new NodeImpl(region2, "B", locationB, Set.of(locationA));
            region2.putNode(nodeA);
            region2.putNode(nodeB);
            return new EdgeImpl(region2, "Test", locationA, locationB, 0);
        };
        Function<Region.Edge, String> testStringFactory = (o) -> o.toString();
        objectUnitTests = new ObjectUnitTests<>(testObjectFactory, testStringFactory);
        objectUnitTests.initialize(10);
        comparableUnitTests = new ComparableUnitTests<>(testObjectFactory);
        comparableUnitTests.initialize(10);

        Location locationA = new Location(0, 0);
        Location locationB = new Location(0, 1);
        Location locationC = new Location(1, 0);

        edgeAA = new EdgeImpl(region1, "AA", locationA, locationA, 0);
        edgeAB = new EdgeImpl(region1, "AB", locationA, locationB, 0);
        edgeBC = new EdgeImpl(region1, "BC", locationB, locationC, 0);

        nodeA = new NodeImpl(region1, "A", locationA, Set.of(locationA, locationB));
        nodeB = new NodeImpl(region1, "B", locationB, Set.of(locationA, locationC));
        nodeC = new NodeImpl(region1, "C", locationC, Set.of(locationB));

        region1.putNode(nodeA);
        region1.putNode(nodeB);
        region1.putNode(nodeC);
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
    public void testGetNode() {
        assertEquals(nodeA, edgeAA.getNodeA());
        assertEquals(nodeA, edgeAB.getNodeA());
        assertEquals(nodeB, edgeBC.getNodeA());
        assertEquals(nodeA, edgeAA.getNodeB());
        assertEquals(nodeB, edgeAB.getNodeB());
        assertEquals(nodeC, edgeBC.getNodeB());
    }
}
