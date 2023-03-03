package projekt.delivery.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import projekt.base.Location;
import projekt.delivery.event.Event;
import projekt.delivery.routing.ConfirmedOrder;
import projekt.delivery.routing.Region;
import projekt.delivery.routing.Vehicle;
import projekt.delivery.routing.VehicleManager;
import projekt.delivery.routing.VehicleManager.OccupiedRestaurant;

/**
 * A very simple delivery service that distributes orders to compatible vehicles
 * in a FIFO manner.
 */
public class BasicDeliveryService extends AbstractDeliveryService {

    // List of orders that have not yet been loaded onto delivery vehicles
    protected final List<ConfirmedOrder> pendingOrders = new ArrayList<>();

    public BasicDeliveryService(
            VehicleManager vehicleManager) {
        super(vehicleManager);
    }

    @Override
    protected List<Event> tick(long currentTick, List<ConfirmedOrder> newOrders) {
        List<Event> vehicleManagerResult = vehicleManager.tick(currentTick);
        pendingOrders.addAll(newOrders);
        pendingOrders
                .sort(Comparator.comparingLong((ConfirmedOrder order) -> order.getDeliveryInterval().start())::compare);
        for (OccupiedRestaurant restaurant : vehicleManager.getOccupiedRestaurants()) {
            for (Vehicle vehicle : restaurant.getVehicles()) {
                Stream<ConfirmedOrder> availableOrders = pendingOrders.stream()
                        .filter(order -> order.getRestaurant() == restaurant);
                List<Location> orderLocations = new ArrayList<>();
                for (ConfirmedOrder order : availableOrders.toList()) {
                    if (vehicle.getCapacity() - (vehicle.getCurrentWeight() + order.getWeight()) <= 0) {
                        break;
                    }
                    restaurant.loadOrder(vehicle, order, currentTick);
                    pendingOrders.remove(order);
                    if (orderLocations.contains(order.getLocation())) {
                        vehicle.moveQueued(vehicleManager.getRegion().getNode(order.getLocation()),
                                (arrivedVehicle, tick) -> {
                                    VehicleManager.OccupiedNeighborhood neighborhood = vehicleManager
                                            .getOccupiedNeighborhood(
                                                    (Region.Node) arrivedVehicle.getOccupied().getComponent());
                                    List<ConfirmedOrder> localOrders = arrivedVehicle.getOrders().stream()
                                            .filter(o -> vehicleManager.getRegion()
                                                    .getNode(o.getLocation()) == neighborhood
                                                            .getComponent())
                                            .toList();
                                    for (ConfirmedOrder loadedOrder : localOrders) {
                                        neighborhood.deliverOrder(arrivedVehicle, loadedOrder, currentTick);
                                    }
                                    if (arrivedVehicle.getOrders().size() == 0) {
                                        arrivedVehicle.moveQueued(restaurant.getComponent());
                                    }
                                });
                        orderLocations.add(order.getLocation());
                    }
                }
            }
        }
        return vehicleManagerResult;
    }

    @Override
    public List<ConfirmedOrder> getPendingOrders() {
        return pendingOrders;
    }

    @Override
    public void reset() {
        super.reset();
        pendingOrders.clear();
    }

    public interface Factory extends DeliveryService.Factory {

        BasicDeliveryService create(VehicleManager vehicleManager);
    }
}
