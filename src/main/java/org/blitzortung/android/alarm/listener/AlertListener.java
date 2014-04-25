package org.blitzortung.android.alarm.listener;

import org.blitzortung.android.alarm.AlarmParameters;
import org.blitzortung.android.alarm.AlarmResult;
import org.blitzortung.android.alarm.object.AlarmStatus;

public interface AlertListener {
    void onAlert(AlarmStatus alarmStatus, AlarmResult alarmResult);

    void onAlertCancel();
}
