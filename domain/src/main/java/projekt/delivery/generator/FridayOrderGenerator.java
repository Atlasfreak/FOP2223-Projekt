package projekt.delivery.generator;

import projekt.base.TickInterval;
import projekt.delivery.routing.ConfirmedOrder;
import projekt.delivery.routing.VehicleManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * An implementation of an {@link OrderGenerator} that represents the incoming
 * orders on an average friday evening.
 * The incoming orders follow a normal distribution.
 * <p>
 *
 * To create a new {@link FridayOrderGenerator} use
 * {@code FridayOrderGenerator.Factory.builder()...build();}.
 */
public class FridayOrderGenerator implements OrderGenerator {

    private final Random random;

    private final double variance;

    private final int orderCount;

    private final VehicleManager vehicleManager;

    private final int deliveryInterval;

    private final double maxWeight;

    private final long lastTick;

    private final Map<Long, List<ConfirmedOrder>> tickToOrder = new HashMap<>();

    /**
     * Creates a new {@link FridayOrderGenerator} with the given parameters.
     *
     * @param orderCount       The total amount of orders this
     *                         {@link OrderGenerator} will create. It is equal to
     *                         the sum of
     *                         the size of the lists that are returned for every
     *                         positive long value.
     * @param vehicleManager   The {@link VehicleManager} this
     *                         {@link OrderGenerator} will create orders for.
     * @param deliveryInterval The amount of ticks between the start and end tick of
     *                         the deliveryInterval of the created orders.
     * @param maxWeight        The maximum weight of a created order.
     * @param standardDeviation The standardDeviation of the normal distribution.
     * @param lastTick         The last tick this {@link OrderGenerator} can return
     *                         a non-empty list.
     * @param seed             The seed for the used {@link Random} instance. If
     *                         negative a random seed will be used.
     */
    private FridayOrderGenerator(int orderCount, VehicleManager vehicleManager, int deliveryInterval, double maxWeight, double standardDeviation, long lastTick, int seed) {
        this.orderCount = orderCount;
        this.vehicleManager = vehicleManager;
        this.deliveryInterval = deliveryInterval;
        this.maxWeight = maxWeight;
        this.lastTick = lastTick;
        this.variance = standardDeviation;
        
        random = seed < 0 ? new Random() : new Random(seed);
    }

    public double randomGaussianBetweenOneAndZero() {
        double randomDouble = random.nextGaussian(0.5, variance);
        while (randomDouble < 0 || randomDouble > 1) {
            randomDouble = random.nextGaussian(0.5, variance);
        }
        return randomDouble;
    }

    @Override
    public List<ConfirmedOrder> generateOrders(long tick) {
        if (tick < 0) {
            throw new IndexOutOfBoundsException(tick);
        }

        if (tickToOrder.size() == 0) {
            populateTickToOrder();
        }
        return tickToOrder.getOrDefault(tick, new ArrayList<>());
    }

    private void populateTickToOrder() {
        List<VehicleManager.OccupiedNeighborhood> neighborhoods = vehicleManager.getOccupiedNeighborhoods().stream()
                .toList();
        List<VehicleManager.OccupiedRestaurant> restaurants = vehicleManager.getOccupiedRestaurants().stream()
                .toList();
        for (int i = 0; i < orderCount; i++) {
            long randomTick = Math.round(randomGaussianBetweenOneAndZero() * lastTick);
            VehicleManager.OccupiedRestaurant restaurant = restaurants.get(random.nextInt(restaurants.size()));

            List<String> foodList = new ArrayList<>();
            List<String> availabeFood = restaurant.getComponent().getAvailableFood();

            int foodListLength = random.nextInt(1, 10);

            for (int j = 0; j < foodListLength; j++) {
                foodList.add(availabeFood.get(random.nextInt(availabeFood.size())));
            }
            List<ConfirmedOrder> confirmedOrders = tickToOrder.getOrDefault(randomTick, new ArrayList<>());

            confirmedOrders.add(new ConfirmedOrder(
                    neighborhoods.get(random.nextInt(neighborhoods.size())).getComponent().getLocation(),
                    restaurant,
                    new TickInterval(randomTick, randomTick + deliveryInterval), foodList,
                    random.nextDouble(maxWeight)));
            tickToOrder.put(randomTick, confirmedOrders);
        }
    }

    /**
     * A {@link OrderGenerator.Factory} for creating a new
     * {@link FridayOrderGenerator}.
     */
    public static class Factory implements OrderGenerator.Factory {

        public final int orderCount;
        public final VehicleManager vehicleManager;
        public final int deliveryInterval;
        public final double maxWeight;
        public final double standardDeviation;
        public final long lastTick;
        public final int seed;


        private Factory(int orderCount, VehicleManager vehicleManager, int deliveryInterval, double maxWeight, double standardDeviation, long lastTick, int seed) {
            this.orderCount = orderCount;
            this.vehicleManager = vehicleManager;
            this.deliveryInterval = deliveryInterval;
            this.maxWeight = maxWeight;
            this.standardDeviation = standardDeviation;
            this.lastTick = lastTick;
            this.seed = seed;
        }

        @Override
        public OrderGenerator create() {
            return new FridayOrderGenerator(orderCount, vehicleManager, deliveryInterval, maxWeight, standardDeviation, lastTick, seed);
        }

        /**
         * Creates a new {@link FridayOrderGenerator.FactoryBuilder}.
         *
         * @return The created {@link FridayOrderGenerator.FactoryBuilder}.
         */
        public static FridayOrderGenerator.FactoryBuilder builder() {
            return new FridayOrderGenerator.FactoryBuilder();
        }
    }

    /**
     * A {@link OrderGenerator.FactoryBuilder} form constructing a new
     * {@link FridayOrderGenerator.Factory}.
     */
    public static class FactoryBuilder implements OrderGenerator.FactoryBuilder {

        public int orderCount = 1000;
        public VehicleManager vehicleManager = null;
        public int deliveryInterval = 15;
        public double maxWeight = 0.5;
        public double standardDeviation = 0.5;
        public long lastTick = 480;
        public int seed = -1;

        private FactoryBuilder() {
        }

        public FactoryBuilder setOrderCount(int orderCount) {
            this.orderCount = orderCount;
            return this;
        }

        public FactoryBuilder setVehicleManager(VehicleManager vehicleManager) {
            this.vehicleManager = vehicleManager;
            return this;
        }

        public FactoryBuilder setDeliveryInterval(int deliveryInterval) {
            this.deliveryInterval = deliveryInterval;
            return this;
        }

        public FactoryBuilder setMaxWeight(double maxWeight) {
            this.maxWeight = maxWeight;
            return this;
        }

        public FactoryBuilder setStandardDeviation(double standardDeviation) {
            this.standardDeviation = standardDeviation;
            return this;
        }

        public FactoryBuilder setLastTick(long lastTick) {
            this.lastTick = lastTick;
            return this;
        }

        public FactoryBuilder setSeed(int seed) {
            this.seed = seed;
            return this;
        }

        @Override
        public Factory build() {
            Objects.requireNonNull(vehicleManager);
            return new Factory(orderCount, vehicleManager, deliveryInterval, maxWeight, standardDeviation, lastTick, seed);
        }
    }
}
