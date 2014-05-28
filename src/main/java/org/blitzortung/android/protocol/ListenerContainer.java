package org.blitzortung.android.protocol;

import java.util.HashSet;
import java.util.Set;

public abstract class ListenerContainer<P, T extends Listener<P>> {

    private final Set<T> listeners;

    private P currentPayload;

    public ListenerContainer() {
        listeners = new HashSet<T>();
    }

    public void addListener(T listener) {
        if (!listeners.contains(listener)) {
            if (listeners.isEmpty()) {
                addedFirstListener();
            }
            listeners.add(listener);
            sendCurrentPayloadTo(listener);
        }
    }

    protected void sendCurrentPayloadTo(T listener) {
        if (currentPayload != null) {
            listener.onUpdated(currentPayload);
        }
    }

    public void removeListener(T listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                removedLastListener();
            }
        }
    }

    public abstract void addedFirstListener();

    public abstract void removedLastListener();

    public void storeAndBroadcast(P payload) {
        currentPayload = payload;
        broadcast(payload);
    }

    public void broadcast(P payload) {
        for (T listener : listeners) {
            listener.onUpdated(payload);
        }
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }
}
