package no.unit.nva.useraccessmanagement.interfaces;

/**
 * Example usage:
 * <br/><pre>{@code
 * MyClass obj = new MyClass.Builder().withFieldA("someValue").withFieldB("someOtherValue");
 * MyClass copy = obj.copy().withFieldB("aThirdValue").build();
 *
 * }</pre>
 *
 * <p>In the above example, {@code MyClass}  has two fields, fieldA, and fieldB.
 * <br/> Object {@code obj} has values {@code "someValue"} and {@code "someOtherValue"} while <br/> Object {@code copy}
 * has values {@code "someValue"} and {@code "aThirdValue"}.
 *
 * @param <T> The Builder class of the class implementing the interface.
 */
public interface WithCopy<T> {

    /**
     * Returns a Builder filled in with a copy of the data of the original object.
     *
     * @return a builder instance with filled in data.
     */
    T copy();
}
