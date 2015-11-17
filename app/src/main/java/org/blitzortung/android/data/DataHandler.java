package org.blitzortung.android.data;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;

import com.annimon.stream.Optional;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.Function;

import org.blitzortung.android.app.Main;
import org.blitzortung.android.app.view.PreferenceKey;
import org.blitzortung.android.data.provider.DataProvider;
import org.blitzortung.android.data.provider.DataProviderFactory;
import org.blitzortung.android.data.provider.DataProviderType;
import org.blitzortung.android.data.provider.result.ClearDataEvent;
import org.blitzortung.android.data.provider.result.DataEvent;
import org.blitzortung.android.data.provider.result.RequestStartedEvent;
import org.blitzortung.android.data.provider.result.ResultEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import lombok.val;

public class DataHandler implements OnSharedPreferenceChangeListener {

    public static final RequestStartedEvent REQUEST_STARTED_EVENT = new RequestStartedEvent();
    public static final ClearDataEvent CLEAR_DATA_EVENT = new ClearDataEvent();
    public static final Set<DataChannel> DEFAULT_DATA_CHANNELS = new HashSet<>();

    static {
        DEFAULT_DATA_CHANNELS.add(DataChannel.STRIKES);
    }

    private final Lock lock = new ReentrantLock();
    private final PackageInfo pInfo;
    private DataProvider dataProvider;
    private String username;
    private String password;
    private Parameters parameters = Parameters.DEFAULT;
    private ParametersController parametersController;
    private Consumer<DataEvent> dataEventConsumer;
    private int preferencesRasterBaselength;
    private int preferencesRegion;
    private DataProviderFactory dataProviderFactory;
    private PowerManager.WakeLock wakeLock;

    public DataHandler(PowerManager.WakeLock wakeLock, SharedPreferences sharedPreferences, PackageInfo pInfo) {
        this(wakeLock, sharedPreferences, pInfo, new DataProviderFactory());
    }

    public DataHandler(PowerManager.WakeLock wakeLock, SharedPreferences sharedPreferences, PackageInfo pInfo,
                       DataProviderFactory dataProviderFactory) {
        this.wakeLock = wakeLock;
        this.dataProviderFactory = dataProviderFactory;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        this.pInfo = pInfo;

        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.DATA_SOURCE);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.USERNAME);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.PASSWORD);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.RASTER_SIZE);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.COUNT_THRESHOLD);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.REGION);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.INTERVAL_DURATION);
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.HISTORIC_TIMESTEP);

        updateProviderSpecifics();
    }

    public void updateDatainBackground() {
        new FetchBackgroundDataTask(wakeLock).execute(TaskParameters.builder().parameters(parameters).updateParticipants(false).build());
    }

    public void updateData() {
        updateData(DEFAULT_DATA_CHANNELS);
    }

    public void updateData(Set<DataChannel> updateTargets) {

        sendEvent(REQUEST_STARTED_EVENT);

        boolean updateParticipants = false;
        if (updateTargets.contains(DataChannel.PARTICIPANTS)) {
            if (dataProvider.getType() == DataProviderType.HTTP || parameters.getRasterBaselength() == 0) {
                updateParticipants = true;
            }
        }

        new FetchDataTask().execute(TaskParameters.builder()
                .parameters(parameters)
                .updateParticipants(updateParticipants)
                .build());
    }

    private void sendEvent(DataEvent dataEvent) {
        if (dataEventConsumer != null) {
            dataEventConsumer.accept(dataEvent);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String keyString) {
        onSharedPreferenceChanged(sharedPreferences, PreferenceKey.fromString(keyString));
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, PreferenceKey key) {
        switch (key) {
            case DATA_SOURCE:
                String providerTypeString = sharedPreferences.getString(key.toString(), DataProviderType.RPC.toString());
                DataProviderType providerType = DataProviderType.valueOf(providerTypeString.toUpperCase());
                dataProvider = dataProviderFactory.getDataProviderForType(providerType);
                dataProvider.setPackageInfo(pInfo);

                updateProviderSpecifics();

                notifyDataReset();
                break;

            case USERNAME:
                username = sharedPreferences.getString(key.toString(), "");
                break;

            case PASSWORD:
                password = sharedPreferences.getString(key.toString(), "");
                break;

            case RASTER_SIZE:
                preferencesRasterBaselength = Integer.parseInt(sharedPreferences.getString(key.toString(), "10000"));
                parameters = parameters.createBuilder().rasterBaselength(preferencesRasterBaselength).build();
                notifyDataReset();
                break;

            case COUNT_THRESHOLD:
                int countThreshold = Integer.parseInt(sharedPreferences.getString(key.toString(), "1"));
                parameters = parameters.createBuilder().countThreshold(countThreshold).build();
                notifyDataReset();
                break;

            case INTERVAL_DURATION:
                parameters = parameters.createBuilder().intervalDuration(Integer.parseInt(sharedPreferences.getString(key.toString(), "60"))).build();
                dataProvider.reset();
                notifyDataReset();
                break;

            case HISTORIC_TIMESTEP:
                parametersController = ParametersController.withOffsetIncrement(
                        Integer.parseInt(sharedPreferences.getString(key.toString(), "30")));
                break;

            case REGION:
                preferencesRegion = Integer.parseInt(sharedPreferences.getString(key.toString(), "1"));
                parameters = parameters.createBuilder().region(preferencesRegion).build();
                dataProvider.reset();
                notifyDataReset();
                break;
        }

        if (dataProvider != null) {
            dataProvider.setCredentials(username, password);
        }
    }

    private void updateProviderSpecifics() {

        DataProviderType providerType = dataProvider.getType();

        switch (providerType) {
            case RPC:
                enableRasterMode();
                break;

            case HTTP:
                disableRasterMode();
                break;
        }
    }

    private void notifyDataReset() {
        sendEvent(CLEAR_DATA_EVENT);
    }

    public void toggleExtendedMode() {
        if (parameters.getRasterBaselength() > 0) {
            disableRasterMode();
        } else {
            enableRasterMode();
        }
        if (!isRealtime()) {
            Set<DataChannel> dataChannels = new HashSet<>();
            dataChannels.add(DataChannel.STRIKES);
            updateData(dataChannels);
        }
    }

    public void disableRasterMode() {
        parameters = parameters.createBuilder().region(0).rasterBaselength(0).build();
    }

    public void enableRasterMode() {
        parameters = parameters.createBuilder().rasterBaselength(preferencesRasterBaselength)
                .region(preferencesRegion).build();
    }

    public void setDataConsumer(Consumer<DataEvent> consumer) {
        this.dataEventConsumer = consumer;
    }

    public int getIntervalDuration() {
        return parameters.getIntervalDuration();
    }

    public boolean ffwdInterval() {
        return updateParameters(parametersController::ffwdInterval);
    }

    public boolean rewInterval() {
        return updateParameters(parametersController::rewInterval);
    }

    public boolean goRealtime() {
        return updateParameters(parametersController::goRealtime);
    }

    public boolean updateParameters(Function<Parameters, Parameters> updater) {
        final Parameters oldParameters = parameters;
        parameters = updater.apply(parameters);
        return !parameters.equals(oldParameters);
    }

    public boolean isRealtime() {
        return parameters.isRealtime();
    }

    public boolean isCapableOfHistoricalData() {
        return dataProvider.isCapableOfHistoricalData();
    }

    public Parameters getParameters() {
        return parameters;
    }

    private class FetchDataTask extends AsyncTask<TaskParameters, Integer, Optional<ResultEvent>> {

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Optional<ResultEvent> result) {
            if (result.isPresent()) {
                final ResultEvent payload = result.get();
                sendEvent(payload);
            }
        }

        @Override
        protected Optional<ResultEvent> doInBackground(TaskParameters... taskParametersArray) {
            val taskParameters = taskParametersArray[0];
            val parameters = taskParameters.getParameters();

            if (lock.tryLock()) {
                try {
                    val result = ResultEvent.builder();

                    dataProvider.setUp();
                    dataProvider.setCredentials(username, password);

                    if (parameters.isRaster()) {
                        dataProvider.getStrikesGrid(parameters, result);
                    } else {
                        dataProvider.getStrikes(parameters, result);
                    }

                    result.parameters(parameters)
                            .referenceTime(System.currentTimeMillis());

                    if (taskParameters.isUpdateParticipants()) {
                        result.stations(dataProvider.getStations(parameters.getRegion()));
                    }

                    dataProvider.shutDown();

                    return Optional.of(result.build());
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return Optional.of(ResultEvent.builder().fail(true).build());
                } finally {
                    lock.unlock();
                }
            }
            return Optional.empty();
        }
    }

    private class FetchBackgroundDataTask extends FetchDataTask {

        private PowerManager.WakeLock wakeLock;

        public FetchBackgroundDataTask(PowerManager.WakeLock wakeLock) {
            super();
            this.wakeLock = wakeLock;
        }

        @Override
        protected void onPostExecute(Optional<ResultEvent> result) {
            super.onPostExecute(result);
            if (wakeLock.isHeld()) {
                try {
                    wakeLock.release();
                    Log.v(Main.LOG_TAG, "FetchBackgroundDataTask released wakelock " + wakeLock);
                } catch (RuntimeException e) {
                    Log.e(Main.LOG_TAG, "FetchBackgroundDataTask release wakelock failed ", e);
                }
            } else {
                Log.e(Main.LOG_TAG, "FetchBackgroundDataTask release wakelock not held ");
            }
        }

        @Override
        protected Optional<ResultEvent> doInBackground(TaskParameters... params) {
            wakeLock.acquire();
            Log.v(Main.LOG_TAG, "FetchBackgroundDataTask aquire wakelock " + wakeLock);
            return super.doInBackground(params);
        }
    }

}
