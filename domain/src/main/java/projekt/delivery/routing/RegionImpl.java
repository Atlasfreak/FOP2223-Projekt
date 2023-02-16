package projekt.delivery.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import projekt.base.DistanceCalculator;
import projekt.base.EuclideanDistanceCalculator;
import projekt.base.Location;

class RegionImpl implements Region {

    private final Map<Location, NodeImpl> nodes = new HashMap<>();
    private final Map<Location, Map<Location, EdgeImpl>> edges = new HashMap<>();
    private final List<EdgeImpl> allEdges = new ArrayList<>();
    private final DistanceCalculator distanceCalculator;

    /**
     * Creates a new, empty {@link RegionImpl} instance using a
     * {@link EuclideanDistanceCalculator}.
     */
    public RegionImpl() {
        this(new EuclideanDistanceCalculator());
    }

    /**
     * Creates a new, empty {@link RegionImpl} instance using the given
     * {@link DistanceCalculator}.
     */
    public RegionImpl(DistanceCalculator distanceCalculator) {
        this.distanceCalculator = distanceCalculator;
    }

    @Override
    public @Nullable Node getNode(Location location) {
        return nodes.get(location);
    }

    @Override
    public @Nullable Edge getEdge(Location locationA, Location locationB) {
        Edge result = null;
        if (edges.containsKey(locationA)) {
            result = edges.get(locationA).get(locationB);
        }
        if (result == null && edges.containsKey(locationB)) {
            result = edges.get(locationB).get(locationB);
        }
        return result;
    }

    @Override
    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    @Override
    public Collection<Edge> getEdges() {
        return Collections.unmodifiableCollection(allEdges);
    }

    @Override
    public DistanceCalculator getDistanceCalculator() {
        return distanceCalculator;
    }

    /**
     * Adds the given {@link NodeImpl} to this {@link RegionImpl}.
     *
     * @param node the {@link NodeImpl} to add.
     */
    void putNode(NodeImpl node) {
        if (node.getRegion() != this) {
            throw new IllegalArgumentException(String.format("Node %s has incorrect region", node.toString()));
        }
        nodes.put(node.getLocation(), node);
    }

    /**
     * Adds the given {@link EdgeImpl} to this {@link RegionImpl}.
     *
     * @param edge the {@link EdgeImpl} to add.
     */
    void putEdge(EdgeImpl edge) {
        if (edge.getNodeA() == null) {
            throw new IllegalArgumentException(
                    String.format("NodeA %s is not part of the region", edge.getLocationA()));
        }
        if (edge.getNodeB() == null) {
            throw new IllegalArgumentException(
                    String.format("NodeB %s is not part of the region", edge.getLocationB()));
        }
        if (edge.getRegion() != this || edge.getNodeA().getRegion() != this || edge.getNodeB().getRegion() != this) {
            throw new IllegalArgumentException(String.format("Edge %s has incorrect region", edge.toString()));
        }
        edges.put(edge.getLocationA(), Map.of(edge.getLocationB(), edge));
        allEdges.add(edge);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof RegionImpl) || o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        RegionImpl castedObject = (RegionImpl) o;
        if (Objects.equals(castedObject.nodes, this.nodes) && Objects.equals(castedObject.edges, this.edges)) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes, allEdges);
    }
}
