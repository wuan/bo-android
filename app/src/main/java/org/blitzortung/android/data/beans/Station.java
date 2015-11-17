package org.blitzortung.android.data.beans;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode
public class Station {

    public static final long OFFLINE_SINCE_NOT_SET = -1;
    private final String name;
    private final float longitude;
    private final float latitude;
    private final long offlineSince;

    public State getState() {
        if (offlineSince == OFFLINE_SINCE_NOT_SET) {
            return State.ON;
        } else {
            long now = System.currentTimeMillis();

            long minutesAgo = (now - offlineSince) / 1000 / 60;

            if (minutesAgo > 24 * 60) {
                return State.OFF;
            } else if (minutesAgo > 15) {
                return State.DELAYED;
            } else {
                return State.ON;
            }
        }
    }

    public enum State {
        ON, DELAYED, OFF
    }
}
