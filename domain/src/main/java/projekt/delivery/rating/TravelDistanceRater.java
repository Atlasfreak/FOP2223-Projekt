package projekt.delivery.rating;

import java.util.Deque;
import java.util.List;

import projekt.delivery.event.ArrivedAtNodeEvent;
import projekt.delivery.event.DeliverOrderEvent;
import projekt.delivery.event.Event;
import projekt.delivery.routing.PathCalculator;
import projekt.delivery.routing.Region;
import projekt.delivery.routing.VehicleManager;
import projekt.delivery.simulation.Simulation;

/**
 * Rates the observed {@link Simulation} based on the distance traveled by all
 * vehicles.
 * <p>
 *
 * To create a new {@link TravelDistanceRater} use
 * {@code TravelDistanceRater.Factory.builder()...build();}.
 */
public class TravelDistanceRater implements Rater {

    public static final RatingCriteria RATING_CRITERIA = RatingCriteria.TRAVEL_DISTANCE;

    private final Region region;
    private final PathCalculator pathCalculator;
    private final double factor;

    private double actualDistance = 0;
    private double worstDistance = 0;

    private TravelDistanceRater(VehicleManager vehicleManager, double factor) {
        region = vehicleManager.getRegion();
        pathCalculator = vehicleManager.getPathCalculator();
        this.factor = factor;
    }

    @Override
    public double getScore() {
        if (0 <= actualDistance && actualDistance < worstDistance * factor) {
            return 1 - (actualDistance / (worstDistance * factor));
        }
        return 0;
    }

    @Override
    public RatingCriteria getRatingCriteria() {
        return RATING_CRITERIA;
    }

    @Override
    public void onTick(List<Event> events, long tick) {
        for (Event event : events) {
            if (event instanceof ArrivedAtNodeEvent) {
                ArrivedAtNodeEvent castedEvent = (ArrivedAtNodeEvent) event;
                actualDistance += castedEvent.getLastEdge().getDuration();
            } else if (event instanceof DeliverOrderEvent) {
                DeliverOrderEvent castedEvent = (DeliverOrderEvent) event;
                Deque<Region.Node> nodesToLocation = pathCalculator.getPath(
                        castedEvent.getOrder().getRestaurant().getComponent(),
                        castedEvent.getNode());
                Region.Node lastNode = castedEvent.getOrder().getRestaurant().getComponent();

                double totalDistance = 0;

                for (Region.Node node : nodesToLocation) {
                    totalDistance += region.getEdge(lastNode, node).getDuration();
                    lastNode = node;
                }
                worstDistance += totalDistance * 2;
            }
        }
    }

    /**
     * A {@link Rater.Factory} for creating a new {@link TravelDistanceRater}.
     */
    public static class Factory implements Rater.Factory {

        public final VehicleManager vehicleManager;
        public final double factor;

        private Factory(VehicleManager vehicleManager, double factor) {
            this.vehicleManager = vehicleManager;
            this.factor = factor;
        }

        @Override
        public TravelDistanceRater create() {
            return new TravelDistanceRater(vehicleManager, factor);
        }

        /**
         * Creates a new {@link TravelDistanceRater.FactoryBuilder}.
         *
         * @return The created {@link TravelDistanceRater.FactoryBuilder}.
         */
        public static FactoryBuilder builder() {
            return new FactoryBuilder();
        }

    }

    /**
     * A {@link Rater.FactoryBuilder} form constructing a new
     * {@link TravelDistanceRater.Factory}.
     */
    public static class FactoryBuilder implements Rater.FactoryBuilder {

        public VehicleManager vehicleManager;
        public double factor = 0.5;

        private FactoryBuilder() {
        }

        @Override
        public Factory build() {
            return new Factory(vehicleManager, factor);
        }

        public FactoryBuilder setVehicleManager(VehicleManager vehicleManager) {
            this.vehicleManager = vehicleManager;
            return this;
        }

        public FactoryBuilder setFactor(double factor) {
            if (factor < 0) {
                throw new IllegalArgumentException("factor must be positive");
            }

            this.factor = factor;
            return this;
        }
    }

}
