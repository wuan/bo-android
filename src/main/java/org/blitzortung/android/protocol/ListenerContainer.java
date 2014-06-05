package org.blitzortung.android.protocol;

import java.util.HashSet;
import java.util.Set;

public abstract class ListenerContainer<P extends Event> {

    private final Set<Listener> listeners;

    private P currentPayload;

    public ListenerContainer() {
        listeners = new HashSet<Listener>();
    }

    public void addListener(Listener listener) {
        if (!listeners.contains(listener)) {
            if (listeners.isEmpty()) {
                addedFirstListener();
            }
            listeners.add(listener);
            sendCurrentPayloadTo(listener);
        }
    }

    protected void sendCurrentPayloadTo(Listener listener) {
        if (currentPayload != null) {
            listener.onEvent(currentPayload);
        }
    }

    public void removeListener(Listener listener) {
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
        for (Listener listener : listeners) {
            listener.onEvent(payload);
        }
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }
}
