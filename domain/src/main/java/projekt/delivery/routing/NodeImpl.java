package projekt.delivery.routing;

import org.jetbrains.annotations.Nullable;
import projekt.base.Location;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class NodeImpl implements Region.Node {

    protected final Set<Location> connections;
    protected final Region region;
    protected final String name;
    protected final Location location;

    /**
     * Creates a new {@link NodeImpl} instance.
     *
     * @param region      The {@link Region} this {@link NodeImpl} belongs to.
     * @param name        The name of this {@link NodeImpl}.
     * @param location    The {@link Location} of this {@link EdgeImpl}.
     * @param connections All {@link Location}s this {@link NeighborhoodImpl} has an
     *                    {@link Region.Edge} to.
     */
    NodeImpl(
            Region region,
            String name,
            Location location,
            Set<Location> connections) {
        this.region = region;
        this.name = name;
        this.location = location;
        this.connections = connections;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public Set<Location> getConnections() {
        return connections;
    }

    @Override
    public @Nullable Region.Edge getEdge(Region.Node other) {
        if (other == null) {
            return null;
        }
        return region.getEdge(location, other.getLocation());
    }

    @Override
    public Set<Region.Node> getAdjacentNodes() {
        Set<Region.Node> result = new HashSet<>();
        for (Location connection : connections) {
            Region.Node node = region.getNode(connection);
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }

    @Override
    public Set<Region.Edge> getAdjacentEdges() {
        Set<Region.Edge> result = new HashSet<>();
        for (Location connection : connections) {
            Region.Edge edge = region.getEdge(location, connection);
            if (edge != null) {
                result.add(edge);
            }
        }
        return result;
    }

    @Override
    public int compareTo(Region.Node o) {
        return location.compareTo(o.getLocation());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof NodeImpl) || o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        NodeImpl castedObject = (NodeImpl) o;
        return Objects.equals(this.location, castedObject.getLocation())
                && Objects.equals(this.name, castedObject.getName())
                && Objects.equals(this.connections, castedObject.getConnections());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, location, connections);
    }

    @Override
    public String toString() {
        return String.format("NodeImpl(name=\'%s\', location=\'%s\', connections=\'%s\')", name, location, connections);
    }
}
