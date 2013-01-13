package org.blitzortung.android.app.controller;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import org.blitzortung.android.app.R;
import org.blitzortung.android.app.TimerTask;
import org.blitzortung.android.data.DataHandler;

import java.util.ArrayList;
import java.util.Collection;


public class HistoryController {

    private final DataHandler dataHandler;
    private ImageButton historyRewind;
    private ImageButton historyForward;
    private ImageButton goRealtime;
    private final Collection<ImageButton> buttons;
    private final TimerTask timerTask;

    private ButtonColumnHandler buttonHandler;

    public HistoryController(final Activity activity, DataHandler dataHandler, TimerTask timerTask) {
        this.timerTask = timerTask;
        this.dataHandler = dataHandler;
        buttons = new ArrayList<ImageButton>();

        setupHistoryRewindButton(activity);
        setupHistoryForwardButton(activity);
        setupGoRealtimeButton(activity);

        setRealtimeData(true)   ;
    }

    public void setButtonHandler(ButtonColumnHandler buttonColumnHandler) {
        this.buttonHandler = buttonColumnHandler;
    }

    public void setRealtimeData(boolean realtimeData) {
        if (dataHandler.isCapableOfHistoricalData()) {
            historyRewind.setVisibility(View.VISIBLE);
            int historyButtonsVisibility = realtimeData ? View.INVISIBLE : View.VISIBLE;
            historyForward.setVisibility(historyButtonsVisibility);
            goRealtime.setVisibility(historyButtonsVisibility);
        } else {
            historyRewind.setVisibility(View.INVISIBLE);
            historyForward.setVisibility(View.INVISIBLE);
            goRealtime.setVisibility(View.INVISIBLE);
        }
    }

    private void setupHistoryRewindButton(final Activity activity) {
        historyRewind = (ImageButton) activity.findViewById(R.id.historyRew);
        buttons.add(historyRewind);
        historyRewind.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (dataHandler.rewInterval()) {
                    disableButtonColumn();
                    historyForward.setVisibility(View.VISIBLE);
                    goRealtime.setVisibility(View.VISIBLE);
                    updateButtonColumn();
                    updateData();
                } else {
                    Toast toast = Toast.makeText(activity.getBaseContext(), activity.getResources().getText(R.string.historic_timestep_limit_reached), 1000);
                    toast.show();
                }
            }
        });
    }

    private void setupHistoryForwardButton(Activity activity) {
        historyForward = (ImageButton) activity.findViewById(R.id.historyFfwd);
        buttons.add(historyForward);
        historyForward.setVisibility(View.INVISIBLE);
        historyForward.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (dataHandler.ffwdInterval()) {
                    if (dataHandler.isRealtime()) {
                        disableButtonColumn();
                        historyForward.setVisibility(View.INVISIBLE);
                        goRealtime.setVisibility(View.INVISIBLE);
                        updateButtonColumn();
                    }
                    updateData();
                }
            }
        });
    }

    private void setupGoRealtimeButton(Activity activity) {
        goRealtime = (ImageButton) activity.findViewById(R.id.goRealtime);
        buttons.add(goRealtime);
        goRealtime.setVisibility(View.INVISIBLE);
        goRealtime.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (dataHandler.goRealtime()) {
                    disableButtonColumn();
                    historyForward.setVisibility(View.INVISIBLE);
                    goRealtime.setVisibility(View.INVISIBLE);
                    updateButtonColumn();
                    timerTask.restart();
                    timerTask.enable();
                }
            }
        });
    }

    private void updateButtonColumn() {
        buttonHandler.updateButtonColumn();
    }

    public Collection<ImageButton> getButtons() {
        return buttons;
    }

    private void disableButtonColumn() {
        buttonHandler.disableButtonColumn();
    }

    private void updateData() {
        DataHandler.UpdateTargets updateTargets = new DataHandler.UpdateTargets();
        updateTargets.updateStrokes();
        dataHandler.updateData(updateTargets);
    }
}
