package lu.kremi151.jenkins.wolagent.util;

/**
 * Represents a supplier of results, that may throw an exception.
 *
 * This is the same as {@link java.util.function.Supplier}, except that this allows the factory to
 * throw an {@link Exception}.
 *
 * @param <T> the type of results supplied by this supplier
 */
@FunctionalInterface
public interface CheckedSupplier<T> {

    /**
     * Gets a result.
     *
     * @return a result
     * @throws Exception if an exception occurs
     */
    T get() throws Exception;

}
