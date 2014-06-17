package org.blitzortung.android.protocol;

import java.util.HashSet;
import java.util.Set;

public abstract class ListenerContainer<P> {

    private final Set<Consumer<P>> consumers;

    private P currentPayload;

    public ListenerContainer() {
        consumers = new HashSet<Consumer<P>>();
    }

    public void addListener(Consumer<P> listener) {
        if (!consumers.contains(listener)) {
            if (consumers.isEmpty()) {
                addedFirstConsumer();
            }
            consumers.add(listener);
            sendCurrentPayloadTo(listener);
        }
    }

    protected void sendCurrentPayloadTo(Consumer<P> consumer) {
        if (currentPayload != null) {
            consumer.consume(currentPayload);
        }
    }

    public void removeListener(Consumer<P> listener) {
        if (consumers.contains(listener)) {
            consumers.remove(listener);
            if (consumers.isEmpty()) {
                removedLastConsumer();
            }
        }
    }

    public abstract void addedFirstConsumer();

    public abstract void removedLastConsumer();

    public void storeAndBroadcast(P payload) {
        currentPayload = payload;
        broadcast(payload);
    }

    public void broadcast(P payload) {
        for (Consumer<P> listener : consumers) {
            listener.consume(payload);
        }
    }

    public boolean isEmpty() {
        return consumers.isEmpty();
    }

    public int size() {
        return consumers.size();
    }

    public Set<Consumer<P>> getConsumers() {
        return consumers;
    }
}
