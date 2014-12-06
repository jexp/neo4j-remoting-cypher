package util;

/**
 * @author mh
 * @since 06.12.14
 */
public interface Converter<F, T> {
    T convert(F value);
}
