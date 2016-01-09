package org.blitzortung.android.alert.event;

import org.blitzortung.android.alert.AlertResult;
import org.blitzortung.android.alert.object.AlertStatus;

public class AlertResultEvent implements AlertEvent {

    private final AlertStatus alertStatus;
    private final AlertResult alertResult;

    public AlertResultEvent(AlertStatus alertStatus, AlertResult alertResult) {
        this.alertStatus = alertStatus;
        this.alertResult = alertResult;
    }

    public AlertStatus getAlertStatus() {
        return alertStatus;
    }

    public AlertResult getAlertResult() {
        return alertResult;
    }
}
