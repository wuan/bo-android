package org.blitzortung.android.util.optional;

public abstract class Optional<T> {

    public abstract T get();

    public abstract boolean isPresent();

    public static <T> Optional<T> fromNullable(T value) {
        if (value != null) {
            return new Some<T>(value);
        } else {
            return new None<T>();
        }
    }

    public static <T> Optional<T> of(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value can not be null");
        }
        return new Some<T>(value);
    }
}
