package projekt.delivery.routing;

import org.jetbrains.annotations.NotNull;
import projekt.base.Location;

import static org.tudalgo.algoutils.student.Student.crash;

import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a weighted edge in a graph.
 */
@SuppressWarnings("ClassCanBeRecord")
class EdgeImpl implements Region.Edge {

    private final Region region;
    private final String name;
    private final Location locationA;
    private final Location locationB;
    private final long duration;

    /**
     * Creates a new {@link EdgeImpl} instance.
     *
     * @param region    The {@link Region} this {@link EdgeImpl} belongs to.
     * @param name      The name of this {@link EdgeImpl}.
     * @param locationA The start of this {@link EdgeImpl}.
     * @param locationB The end of this {@link EdgeImpl}.
     * @param duration  The length of this {@link EdgeImpl}.
     */
    EdgeImpl(
            Region region,
            String name,
            Location locationA,
            Location locationB,
            long duration) {
        this.region = region;
        this.name = name;
        // locations must be in ascending order
        if (locationA.compareTo(locationB) > 0) {
            throw new IllegalArgumentException(
                    String.format("locationA %s must be <= locationB %s", locationA, locationB));
        }
        this.locationA = locationA;
        this.locationB = locationB;
        this.duration = duration;
    }

    /**
     * Returns the start of this {@link EdgeImpl}.
     *
     * @return The start of this {@link EdgeImpl}.
     */
    public Location getLocationA() {
        return locationA;
    }

    /**
     * Returns the end of this {@link EdgeImpl}.
     *
     * @return The end of this {@link EdgeImpl}.
     */
    public Location getLocationB() {
        return locationB;
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
    public long getDuration() {
        return duration;
    }

    @Override
    public Region.Node getNodeA() {
        return region.getNode(locationA);
    }

    @Override
    public Region.Node getNodeB() {
        return region.getNode(locationB);
    }

    @Override
    public int compareTo(Region.@NotNull Edge o) {
        Comparator<Region.Edge> nodeAComparator = Comparator.comparing(edge -> edge.getNodeA());
        Comparator<Region.Edge> nodeBComparator = Comparator.comparing(edge -> edge.getNodeB());
        return nodeAComparator.thenComparing(nodeBComparator).compare(this, o);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EdgeImpl) || o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        EdgeImpl castedObject = (EdgeImpl) o;
        return Objects.equals(this.name, castedObject.name) && Objects.equals(this.locationA, castedObject.locationA)
                && Objects.equals(this.locationB, castedObject.locationB)
                && Objects.equals(this.duration, castedObject.duration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, locationA, locationB, duration);
    }

    @Override
    public String toString() {
        return String.format("EdgeImpl(name=\'%s\', locationA=\'%s\', locationB=\'%s\', duration=\'%s\')", name,
                locationA, locationB, duration);
    }
}
