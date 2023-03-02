package projekt.delivery.rating;

import java.util.List;

import projekt.base.TickInterval;
import projekt.delivery.event.DeliverOrderEvent;
import projekt.delivery.event.Event;
import projekt.delivery.event.OrderReceivedEvent;
import projekt.delivery.routing.ConfirmedOrder;
import projekt.delivery.simulation.Simulation;

/**
 * Rates the observed {@link Simulation} based on the punctuality of the orders.
 * <p>
 *
 * To create a new {@link InTimeRater} use
 * {@code InTimeRater.Factory.builder()...build();}.
 */
public class InTimeRater implements Rater {

    public static final RatingCriteria RATING_CRITERIA = RatingCriteria.IN_TIME;

    private final long ignoredTicksOff;
    private final long maxTicksOff;

    private double maxTotalTicksOff = 0;
    private double actualTotalTicksOff = 0;

    /**
     * Creates a new {@link InTimeRater} instance.
     *
     * @param ignoredTicksOff The amount of ticks this {@link InTimeRater} ignores
     *                        when dealing with an {@link ConfirmedOrder} that
     *                        didn't get delivered in time.
     * @param maxTicksOff     The maximum amount of ticks too late/early this
     *                        {@link InTimeRater} considers.
     */
    private InTimeRater(long ignoredTicksOff, long maxTicksOff) {
        if (ignoredTicksOff < 0)
            throw new IllegalArgumentException(String.valueOf(ignoredTicksOff));
        if (maxTicksOff <= 0)
            throw new IllegalArgumentException(String.valueOf(maxTicksOff));

        this.ignoredTicksOff = ignoredTicksOff;
        this.maxTicksOff = maxTicksOff;
    }

    @Override
    public double getScore() {
        if (maxTotalTicksOff != 0) {
            return 1 - (actualTotalTicksOff / maxTotalTicksOff);
        }
        return 0;
    }

    @Override
    public void onTick(List<Event> events, long tick) {
        for (Event event : events) {
            if (event instanceof DeliverOrderEvent) {
                DeliverOrderEvent castedEvent = (DeliverOrderEvent) event;
                long actualDeliveryTick = castedEvent.getOrder().getActualDeliveryTick();
                TickInterval expectedDeliveryInterval = castedEvent.getOrder().getDeliveryInterval();
                long deliveryDifference = 0;
                if (actualDeliveryTick > expectedDeliveryInterval.end() + ignoredTicksOff) {
                    deliveryDifference = actualDeliveryTick - expectedDeliveryInterval.end() - ignoredTicksOff;
                } else if (actualDeliveryTick < expectedDeliveryInterval.start() - ignoredTicksOff) {
                    deliveryDifference = expectedDeliveryInterval.start() - ignoredTicksOff - actualDeliveryTick;
                }

                if (deliveryDifference > maxTicksOff) {
                    deliveryDifference = maxTicksOff;
                }
                actualTotalTicksOff -= maxTicksOff;
                actualTotalTicksOff += deliveryDifference;
            } else if (event instanceof OrderReceivedEvent) {
                maxTotalTicksOff += maxTicksOff;
                actualTotalTicksOff += maxTicksOff;

            }
        }
    }

    /**
     * A {@link Rater.Factory} for creating a new {@link InTimeRater}.
     */
    @Override
    public RatingCriteria getRatingCriteria() {
        return RATING_CRITERIA;
    }

    public static class Factory implements Rater.Factory {

        public final long ignoredTicksOff;
        public final long maxTicksOff;

        private Factory(long ignoredTicksOff, long maxTicksOff) {
            this.ignoredTicksOff = ignoredTicksOff;
            this.maxTicksOff = maxTicksOff;
        }

        @Override
        public InTimeRater create() {
            return new InTimeRater(ignoredTicksOff, maxTicksOff);
        }

        /**
         * Creates a new {@link InTimeRater.FactoryBuilder}.
         *
         * @return The created {@link InTimeRater.FactoryBuilder}.
         */
        public static FactoryBuilder builder() {
            return new FactoryBuilder();
        }
    }

    /**
     * A {@link Rater.FactoryBuilder} form constructing a new
     * {@link InTimeRater.Factory}.
     */
    public static class FactoryBuilder implements Rater.FactoryBuilder {

        public long ignoredTicksOff = 5;
        public long maxTicksOff = 25;

        private FactoryBuilder() {
        }

        public FactoryBuilder setIgnoredTicksOff(long ignoredTicksOff) {
            this.ignoredTicksOff = ignoredTicksOff;
            return this;
        }

        public FactoryBuilder setMaxTicksOff(long maxTicksOff) {
            this.maxTicksOff = maxTicksOff;
            return this;
        }

        @Override
        public Factory build() {
            return new Factory(ignoredTicksOff, maxTicksOff);
        }
    }
}
