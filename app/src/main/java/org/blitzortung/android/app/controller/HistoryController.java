package org.blitzortung.android.app.controller;

import android.app.Activity;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.annimon.stream.function.Consumer;

import org.blitzortung.android.app.AppService;
import org.blitzortung.android.app.R;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.provider.result.DataEvent;
import org.blitzortung.android.data.provider.result.ResultEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import lombok.val;


public class HistoryController {

    private final Collection<ImageButton> buttons;
    private ImageButton historyRewind;
    private ImageButton historyForward;
    private ImageButton goRealtime;
    private AppService appService;

    private ButtonColumnHandler buttonHandler;
    private DataHandler dataHandler;
    private Consumer<DataEvent> dataEventConsumer = event -> {
        if (event instanceof ResultEvent) {
            ResultEvent resultEvent = (ResultEvent) event;
            if (!resultEvent.hasFailed()) {
                setRealtimeData(resultEvent.containsRealtimeData());
            }
        }
    };

    public HistoryController(final Activity activity) {
        buttons = new ArrayList<>();

        setupHistoryRewindButton(activity);
        setupHistoryForwardButton(activity);
        setupGoRealtimeButton(activity);

        setRealtimeData(true);
    }

    public void setButtonHandler(ButtonColumnHandler buttonColumnHandler) {
        this.buttonHandler = buttonColumnHandler;
    }

    public void setRealtimeData(boolean realtimeData) {
        if (appService != null && dataHandler.isCapableOfHistoricalData()) {
            historyRewind.setVisibility(View.VISIBLE);
            int historyButtonsVisibility = realtimeData ? View.INVISIBLE : View.VISIBLE;
            historyForward.setVisibility(historyButtonsVisibility);
            goRealtime.setVisibility(historyButtonsVisibility);
        } else {
            historyRewind.setVisibility(View.INVISIBLE);
            historyForward.setVisibility(View.INVISIBLE);
            goRealtime.setVisibility(View.INVISIBLE);
        }
        updateButtonColumn();
    }

    private void setupHistoryRewindButton(final Activity activity) {
        historyRewind = (ImageButton) activity.findViewById(R.id.historyRew);
        buttons.add(historyRewind);
        historyRewind.setOnClickListener(v -> {
            if (dataHandler.rewInterval()) {
                disableButtonColumn();
                historyForward.setVisibility(View.VISIBLE);
                goRealtime.setVisibility(View.VISIBLE);
                updateButtonColumn();
                updateData();
            } else {
                Toast toast = Toast.makeText(activity.getBaseContext(), activity.getResources().getText(R.string.historic_timestep_limit_reached), Toast.LENGTH_SHORT);
                toast.show();
            }
        });
    }

    private void setupHistoryForwardButton(Activity activity) {
        historyForward = (ImageButton) activity.findViewById(R.id.historyFfwd);
        buttons.add(historyForward);
        historyForward.setVisibility(View.INVISIBLE);
        historyForward.setOnClickListener(v -> {
            if (dataHandler.ffwdInterval()) {
                if (dataHandler.isRealtime()) {
                    configureForRealtimeOperation();
                } else {
                    dataHandler.updateData();
                }
            }
        });
    }

    private void setupGoRealtimeButton(Activity activity) {
        goRealtime = (ImageButton) activity.findViewById(R.id.goRealtime);
        buttons.add(goRealtime);
        goRealtime.setVisibility(View.INVISIBLE);
        goRealtime.setOnClickListener(v -> {
            if (dataHandler.goRealtime()) {
                configureForRealtimeOperation();
            }
        });
    }

    private void configureForRealtimeOperation() {
        disableButtonColumn();
        historyForward.setVisibility(View.INVISIBLE);
        goRealtime.setVisibility(View.INVISIBLE);
        updateButtonColumn();
        appService.restart();
    }

    private void updateButtonColumn() {
        if (buttonHandler != null) {
            buttonHandler.updateButtonColumn();
        }
    }

    public Collection<ImageButton> getButtons() {
        return buttons;
    }

    private void disableButtonColumn() {
        buttonHandler.lockButtonColumn();
    }

    private void updateData() {
        dataHandler.updateData(Collections.singleton(DataChannel.STRIKES));
    }

    public void setAppService(AppService appService) {
        this.appService = appService;
        dataHandler = appService != null ? appService.getDataHandler() : null;
    }

    public Consumer<DataEvent> getDataConsumer() {
        return dataEventConsumer;
    }
}
