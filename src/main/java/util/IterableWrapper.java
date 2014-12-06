package util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 06.12.14
 */
public abstract class IterableWrapper<F, T> implements Iterable<T>, Converter<F, T> {
    private Iterable<F> iterable;

    public IterableWrapper(Iterable<F> iterable) {
        this.iterable = iterable;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorWrapper<F, T>(this.iterable.iterator()) {
            @Override
            public T convert(F value) {
                return IterableWrapper.this.convert(value);
            }
        };
    }
}
