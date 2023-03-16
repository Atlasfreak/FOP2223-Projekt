package projekt.base;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import projekt.ComparableUnitTests;
import projekt.ObjectUnitTests;

import java.util.function.Function;

public class LocationUnitTests {

    private static ComparableUnitTests<Location> comparableUnitTests;
    private static ObjectUnitTests<Location> objectUnitTests;

    @BeforeAll
    public static void initialize() {
        Function<Integer, Location> testObjectFactory = (i) -> new Location(i, i * i);
        Function<Location, String> testStringFactory = (o) -> o.toString();
        objectUnitTests = new ObjectUnitTests<>(testObjectFactory, testStringFactory);
        objectUnitTests.initialize(10);
        comparableUnitTests = new ComparableUnitTests<>(testObjectFactory);
        comparableUnitTests.initialize(10);
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
}
