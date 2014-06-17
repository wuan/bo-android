package org.blitzortung.android.protocol;

public interface Consumer<P> {
    public void consume(P payload);
}
