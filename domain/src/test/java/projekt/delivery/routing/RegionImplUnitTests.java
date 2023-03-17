package projekt.delivery.routing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import projekt.ObjectUnitTests;
import projekt.base.EuclideanDistanceCalculator;
import projekt.base.Location;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class RegionImplUnitTests {

    private static ObjectUnitTests<RegionImpl> objectUnitTests;

    @BeforeAll
    public static void initialize() {
        Function<Integer, RegionImpl> testObjectFactory = (i) -> {
            Location location1 = new Location(i + 1, (i + 1) * i);
            Location location2 = new Location(-i, -1 * ((i + 1) * i));
            return (RegionImpl) Region.builder().addNode("A", location1).addNode("B", location2)
                    .addEdge("AB", location1, location2).distanceCalculator(new EuclideanDistanceCalculator()).build();
        };
        Function<RegionImpl, String> testStringFactory = (o) -> null;
        objectUnitTests = new ObjectUnitTests<>(testObjectFactory, testStringFactory);
        objectUnitTests.initialize(10);
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
    public void testNodes() {
        RegionImpl region1 = new RegionImpl();
        RegionImpl region2 = new RegionImpl();

        Location locationA = new Location(0, 0);
        NodeImpl nodeA = new NodeImpl(region1, "A", locationA, null);

        Location locationB = new Location(0, 1);
        NodeImpl nodeB = new NodeImpl(region1, "B", locationB, null);

        Location locationC = new Location(1, 0);
        NodeImpl nodeC = new NodeImpl(region1, "C", locationC, null);

        Location locationD = new Location(0, 0);
        NodeImpl nodeD = new NodeImpl(region2, "D", locationD, null);

        region1.putNode(nodeA);
        region1.putNode(nodeB);
        region1.putNode(nodeC);

        // Test node correctly added to region
        assertTrue(region1.getNodes().contains(nodeA));
        assertTrue(region1.getNodes().contains(nodeB));
        assertTrue(region1.getNodes().contains(nodeC));

        // Test correct node gets return for corresponding location
        assertEquals(nodeA, region1.getNode(locationA));
        assertEquals(nodeB, region1.getNode(locationB));
        assertEquals(nodeC, region1.getNode(locationC));

        // Test return null, when no Node for location is in the region
        assertNull(region1.getNode(new Location(42, 69)));

        // Test throws correct Exception, when node is in different region
        assertThrows(IllegalArgumentException.class, () -> region1.putNode(nodeD));
    }

    @Test
    public void testEdges() {
        RegionImpl region1 = new RegionImpl();
        RegionImpl region2 = new RegionImpl();

        Location locationA = new Location(0, 0);
        NodeImpl nodeA = new NodeImpl(region1, "A", locationA, null);

        Location locationB = new Location(0, 1);
        NodeImpl nodeB = new NodeImpl(region1, "B", locationB, null);

        Location locationC = new Location(1, 0);
        NodeImpl nodeC = new NodeImpl(region1, "C", locationC, null);

        Location locationD = new Location(-1, -1);

        Location locationE = new Location(2, 2);

        EdgeImpl edgeAA = new EdgeImpl(region1, "AA", locationA, locationA, 0);
        EdgeImpl edgeAB = new EdgeImpl(region1, "AB", locationA, locationB, 0);
        EdgeImpl edgeBC = new EdgeImpl(region1, "BC", locationB, locationC, 0);
        EdgeImpl edgeABRegion2 = new EdgeImpl(region2, "AB", locationA, locationB, 0);
        EdgeImpl edgeDBRegion1 = new EdgeImpl(region1, "DB", locationD, locationB, 0);
        EdgeImpl edgeAERegion1 = new EdgeImpl(region1, "AE", locationA, locationE, 0);

        region1.putNode(nodeA);
        region1.putNode(nodeB);
        region1.putNode(nodeC);
        region1.putEdge(edgeAA);
        region1.putEdge(edgeAB);
        region1.putEdge(edgeBC);

        assertEquals(edgeAA, region1.getEdge(locationA, locationA));
        assertEquals(edgeAB, region1.getEdge(locationA, locationB));
        assertEquals(edgeBC, region1.getEdge(locationB, locationC));

        assertTrue(region1.getEdges().contains(edgeAA));
        assertTrue(region1.getEdges().contains(edgeAB));
        assertTrue(region1.getEdges().contains(edgeBC));

        assertNull(region1.getEdge(locationA, locationC));

        assertThrows(IllegalArgumentException.class, () -> region1.putEdge(edgeABRegion2));

        assertThrows(IllegalArgumentException.class, () -> region1.putEdge(edgeDBRegion1));

        assertThrows(IllegalArgumentException.class, () -> region1.putEdge(edgeAERegion1));
    }
}
