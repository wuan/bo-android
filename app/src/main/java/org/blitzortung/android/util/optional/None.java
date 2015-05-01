package org.blitzortung.android.util.optional;

public class None<T> extends Optional<T> {

    @Override
    public T get() {
        throw new IllegalStateException("get() should not be called on None-Object");
    }

    @Override
    public boolean isPresent() {
        return false;
    }
}
