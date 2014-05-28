package org.blitzortung.android.util.optional;

public class Some<T> extends Optional<T> {

    private final T value;

    public Some(T value) {
        if (value == null) {
            throw new IllegalArgumentException("value should be set");
        }

        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public boolean isPresent() {
        return true;
    }
}
