package org.blitzortung.android.app.controller;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import org.blitzortung.android.app.AppService;
import org.blitzortung.android.app.R;
import org.blitzortung.android.data.DataChannel;
import org.blitzortung.android.data.DataHandler;
import org.blitzortung.android.data.provider.result.DataEvent;
import org.blitzortung.android.data.provider.result.ResultEvent;
import org.blitzortung.android.protocol.Consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class HistoryController {

    private ImageButton historyRewind;
    private ImageButton historyForward;
    private ImageButton goRealtime;
    private final Collection<ImageButton> buttons;
    private AppService appService;

    private ButtonColumnHandler buttonHandler;
    private DataHandler dataHandler;

    public HistoryController(final Activity activity) {
        buttons = new ArrayList<ImageButton>();

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
                        configureForRealtimeOperation();
                    } else {
                        dataHandler.updateData();
                    }
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
                    configureForRealtimeOperation();
                }
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
        Set<DataChannel> dataChannels = new HashSet<DataChannel>();
        dataChannels.add(DataChannel.STRIKES);
        dataHandler.updateData(dataChannels);
    }

    public void setAppService(AppService appService) {
        this.appService = appService;
        dataHandler = appService != null ? appService.getDataHandler() : null;
    }

    private Consumer<DataEvent> dataEventConsumer = new Consumer<DataEvent>() {
        @Override
        public void consume(DataEvent event) {
            if (event instanceof ResultEvent) {
                ResultEvent resultEvent = (ResultEvent) event;
                if (!resultEvent.hasFailed()) {
                    setRealtimeData(resultEvent.containsRealtimeData());
                }
            }
        }
    };

    public Consumer<DataEvent> getDataConsumer() {
        return dataEventConsumer;
    }
}
