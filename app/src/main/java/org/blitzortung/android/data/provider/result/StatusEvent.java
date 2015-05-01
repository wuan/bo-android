package org.blitzortung.android.data.provider.result;

public class StatusEvent implements DataEvent {
    private final String status;

    public StatusEvent(String status) {

        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
