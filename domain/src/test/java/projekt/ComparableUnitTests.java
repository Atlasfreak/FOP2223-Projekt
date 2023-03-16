package projekt;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class ComparableUnitTests<T extends Comparable<? super T>> {

    private final Function<Integer, T> testObjectFactory;

    private T[] testObjects;

    public ComparableUnitTests(Function<Integer, T> testObjectFactory) {
        this.testObjectFactory = testObjectFactory;
    }

    @SuppressWarnings("unchecked")
    public void initialize(int testObjectCount) {
        testObjects = (T[]) new Object[testObjectCount];
        for (int i = 0; i < testObjects.length; i++) {
            testObjects[i] = testObjectFactory.apply(i);
        }
    }

    public void testBiggerThen() {
        for (int i = 0; i < testObjects.length; i++) {
            for (int j = 0; j < i; j++) {
                assertEquals(testObjects[i].compareTo(testObjects[j]), 1);
            }
        }
    }

    @SuppressWarnings("EqualsWithItself")
    public void testAsBigAs() {
        for (int i = 0; i < testObjects.length; i++) {
            assertEquals(testObjects[i].compareTo(testObjects[i]), 0);
        }
    }

    public void testLessThen() {
        for (int i = 0; i < testObjects.length; i++) {
            for (int j = 0; j < i; j++) {
                assertEquals(testObjects[i].compareTo(testObjects[j]), -1);
            }
        }
    }
}
