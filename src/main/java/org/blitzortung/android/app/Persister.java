package org.blitzortung.android.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import org.blitzortung.android.data.provider.DataResult;
import org.blitzortung.android.map.overlay.ParticipantsOverlay;
import org.blitzortung.android.map.overlay.StrokesOverlay;
import org.blitzortung.android.map.overlay.color.ParticipantColorHandler;
import org.blitzortung.android.map.overlay.color.StrokeColorHandler;

public class Persister {

    private final StrokesOverlay strokesOverlay;

    private final ParticipantsOverlay participantsOverlay;

    private DataResult currentResult;

    public Persister(Activity activity, SharedPreferences sharedPreferences, PackageInfo pInfo) {
        strokesOverlay = new StrokesOverlay(activity, new StrokeColorHandler(sharedPreferences));
        participantsOverlay = new ParticipantsOverlay(activity, new ParticipantColorHandler(sharedPreferences));
    }

    public void updateContext(Main mainActivity) {
        strokesOverlay.setActivity(mainActivity);
        participantsOverlay.setActivity(mainActivity);
    }

    public StrokesOverlay getStrokesOverlay() {
        return strokesOverlay;
    }

    public ParticipantsOverlay getParticipantsOverlay() {
        return participantsOverlay;
    }

    public void setCurrentResult(DataResult result) {
        currentResult = result;
    }

    public DataResult getCurrentResult() {
        return currentResult;
    }

    public boolean hasCurrentResult() {
        return currentResult != null;
    }
}
