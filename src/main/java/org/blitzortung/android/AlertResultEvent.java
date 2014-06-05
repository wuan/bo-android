package org.blitzortung.android;

import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.alarm.AlertEvent;
import org.blitzortung.android.alarm.object.AlarmStatus;

public class AlertResultEvent implements AlertEvent {

    private final AlarmStatus alarmStatus;
    private final AlarmResult alarmResult;

    public AlertResultEvent(AlarmStatus alarmStatus, AlarmResult alarmResult) {
        this.alarmStatus = alarmStatus;
        this.alarmResult = alarmResult;
    }

    public AlarmStatus getAlertStatus() {
        return alarmStatus;
    }

    public AlarmResult getAlertResult() {
        return alarmResult;
    }
}
