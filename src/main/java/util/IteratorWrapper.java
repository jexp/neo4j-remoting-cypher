package util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author mh
 * @since 06.12.14
 */
public abstract class IteratorWrapper<F,T> implements Iterator<T>,Converter<F,T> {
    private Iterator<F> iterator;

    public IteratorWrapper(Iterator<F> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public T next() {
        return convert(this.iterator.next());
    }

    @Override
    public void remove() {
        this.iterator.remove();
    }
}
