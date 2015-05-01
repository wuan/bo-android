package org.blitzortung.android.protocol;

import java.util.HashSet;
import java.util.Set;

public abstract class ConsumerContainer<P> {

    private final Set<Consumer<P>> consumers;

    private P currentPayload;

    public ConsumerContainer() {
        consumers = new HashSet<Consumer<P>>();
    }

    public void addConsumer(Consumer<P> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("consumer may not be null");
        }

        if (!consumers.contains(consumer)) {
            final boolean isFirst = consumers.isEmpty();
            consumers.add(consumer);
            if (isFirst) {
                addedFirstConsumer();
            }
            sendCurrentPayloadTo(consumer);
        }
    }

    protected void sendCurrentPayloadTo(Consumer<P> consumer) {
        if (currentPayload != null) {
            consumer.consume(currentPayload);
        }
    }

    public void removeConsumer(Consumer<P> consumer) {
        if (consumers.contains(consumer)) {
            consumers.remove(consumer);
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
        for (Consumer<P> consumer : consumers) {
            consumer.consume(payload);
        }
    }

    public boolean isEmpty() {
        return consumers.isEmpty();
    }

    public int size() {
        return consumers.size();
    }
}
