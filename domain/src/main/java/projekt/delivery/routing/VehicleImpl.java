package projekt.delivery.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import projekt.delivery.routing.Region.Edge;

class VehicleImpl implements Vehicle {

    private final int id;
    private final double capacity;
    private final List<ConfirmedOrder> orders = new ArrayList<>();
    private final VehicleManagerImpl vehicleManager;
    private final Deque<PathImpl> moveQueue = new LinkedList<>();
    private final VehicleManager.OccupiedRestaurant startingNode;
    private AbstractOccupied<?> occupied;

    public VehicleImpl(
            int id,
            double capacity,
            VehicleManagerImpl vehicleManager,
            VehicleManager.OccupiedRestaurant startingNode) {
        this.id = id;
        this.capacity = capacity;
        this.occupied = (AbstractOccupied<?>) startingNode;
        this.vehicleManager = vehicleManager;
        this.startingNode = startingNode;
    }

    @Override
    public VehicleManager.Occupied<?> getOccupied() {
        return occupied;
    }

    @Override
    public @Nullable VehicleManager.Occupied<?> getPreviousOccupied() {
        AbstractOccupied.VehicleStats stats = occupied.vehicles.get(this);
        return stats == null ? null : stats.previous;
    }

    @Override
    public List<? extends Path> getPaths() {
        return new LinkedList<>(moveQueue);
    }

    void setOccupied(AbstractOccupied<?> occupied) {
        this.occupied = occupied;
    }

    @Override
    public void moveDirect(Region.Node node, BiConsumer<? super Vehicle, Long> arrivalAction) {
        if (node == occupied) {
            throw new IllegalArgumentException();
        }
        PathImpl pathToNode = null;
        if (occupied instanceof Edge) {
            pathToNode = moveQueue.getFirst();
        }
        moveQueue.clear();
        moveQueue.add(pathToNode);
        moveQueued(node, arrivalAction);
    }

    @Override
    public void moveQueued(Region.Node node, BiConsumer<? super Vehicle, Long> arrivalAction) {
        if (node == occupied && moveQueue.size() <= 1) {
            throw new IllegalArgumentException();
        }
        moveQueue.add(new PathImpl(
                vehicleManager.getPathCalculator().getPath(moveQueue.getLast().nodes.getLast(), node), arrivalAction));
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public double getCapacity() {
        return capacity;
    }

    @Override
    public VehicleManager getVehicleManager() {
        return vehicleManager;
    }

    @Override
    public VehicleManager.Occupied<? extends Region.Node> getStartingNode() {
        return startingNode;
    }

    @Override
    public Collection<ConfirmedOrder> getOrders() {
        return orders;
    }

    @Override
    public void reset() {
        occupied = (AbstractOccupied<?>) startingNode;
        moveQueue.clear();
        orders.clear();
    }

    private void checkMoveToNode(Region.Node node) {
        if (occupied.component.equals(node) && moveQueue.isEmpty()) {
            throw new IllegalArgumentException("Vehicle " + getId() + " cannot move to own node " + node);
        }
    }

    void move(long currentTick) {
        final Region region = vehicleManager.getRegion();
        if (moveQueue.isEmpty()) {
            return;
        }
        final PathImpl path = moveQueue.peek();
        if (path.nodes().isEmpty()) {
            moveQueue.pop();
            final @Nullable BiConsumer<? super Vehicle, Long> action = path.arrivalAction();
            if (action == null) {
                move(currentTick);
            } else {
                action.accept(this, currentTick);
            }
        } else {
            Region.Node next = path.nodes().peek();
            if (occupied instanceof OccupiedNodeImpl) {
                vehicleManager.getOccupied(region.getEdge(((OccupiedNodeImpl<?>) occupied).getComponent(), next))
                        .addVehicle(this, currentTick);
            } else if (occupied instanceof OccupiedEdgeImpl) {
                vehicleManager.getOccupied(next).addVehicle(this, currentTick);
                path.nodes().pop();
            } else {
                throw new AssertionError("Component must be either node or component");
            }
        }
    }

    void loadOrder(ConfirmedOrder order) {
        if (getCurrentWeight() + order.getWeight() > capacity) {
            throw new VehicleOverloadedException(this, getCurrentWeight() + order.getWeight());
        }
        orders.add(order);
    }

    void unloadOrder(ConfirmedOrder order) {
        orders.remove(order);
    }

    @Override
    public int compareTo(Vehicle o) {
        return Integer.compare(getId(), o.getId());
    }

    @Override
    public String toString() {
        return "VehicleImpl("
                + "id=" + id
                + ", capacity=" + capacity
                + ", orders=" + orders
                + ", component=" + occupied.component
                + ')';
    }

    private record PathImpl(Deque<Region.Node> nodes, BiConsumer<? super Vehicle, Long> arrivalAction) implements Path {

    }
}
