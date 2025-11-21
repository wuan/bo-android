# ViewModel Migration Example

This guide shows how to migrate from direct handler injection to using ViewModels in your activities and fragments.

## Before & After Comparison

### BEFORE: Direct Handler Injection
```kotlin
class Main : FragmentActivity() {
    // Direct injection of handlers
    @set:Inject
    internal lateinit var locationHandler: LocationHandler

    @set:Inject
    internal lateinit var alertHandler: AlertHandler

    @set:Inject
    internal lateinit var dataHandler: MainDataHandler

    // Callback-based data updates
    private val dataEventConsumer: (DataEvent) -> Unit = { event ->
        when (event) {
            is RequestStartedEvent -> {
                statusComponent.startProgress()
            }
            is ResultEvent -> {
                currentResult = event
                updateUI(event)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Manual registration
        dataHandler.requestUpdates(dataEventConsumer)
        locationHandler.requestUpdates(locationEventConsumer)
    }

    override fun onStop() {
        super.onStop()
        // Manual cleanup
        dataHandler.removeUpdates(dataEventConsumer)
        locationHandler.removeUpdates(locationEventConsumer)
    }
}
```

### AFTER: ViewModel-Based Approach
```kotlin
class Main : FragmentActivity() {
    // Inject ViewModelFactory (only once)
    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    // Keep other non-data dependencies
    @set:Inject
    internal lateinit var preferences: SharedPreferences

    @set:Inject
    internal lateinit var versionComponent: VersionComponent

    // ViewModel is lifecycle-aware
    private val viewModel: MainViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        // Set up UI
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observe data reactively
        observeViewModel()

        // Start data updates
        viewModel.start()
    }

    private fun observeViewModel() {
        // Observe data events using lifecycleScope
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            statusComponent.startProgress()
                        } else {
                            statusComponent.stopProgress()
                        }
                    }
                }

                // Observe error state
                launch {
                    viewModel.hasError.collect { hasError ->
                        statusComponent.indicateError(hasError)
                    }
                }

                // Observe result data
                launch {
                    viewModel.currentResult.collect { result ->
                        result?.let { updateUI(it) }
                    }
                }

                // Observe location updates
                launch {
                    viewModel.locationEvents.collect { locationEvent ->
                        locationEvent?.let {
                            updateLocation(it.location)
                        }
                    }
                }

                // Observe alert events
                launch {
                    viewModel.alertEvents.collect { alertEvent ->
                        alertEvent?.let { handleAlert(it) }
                    }
                }
            }
        }
    }

    private fun updateUI(result: ResultEvent) {
        // Same UI update logic as before
        val resultParameters = result.parameters

        clearDataIfRequested()

        val initializeOverlay = strikeListOverlay.parameters != resultParameters
        with(strikeListOverlay) {
            parameters = resultParameters
            gridParameters = result.gridParameters
            referenceTime = result.referenceTime
        }

        if (result.updated >= 0 && !initializeOverlay) {
            strikeListOverlay.expireStrikes()
        } else {
            strikeListOverlay.clear()
        }

        result.strikes?.let { strikes ->
            val strikesToAdd = if (result.updated > 0 && !initializeOverlay) {
                strikes.takeLast(result.updated)
            } else {
                strikes
            }
            strikeListOverlay.addStrikes(strikesToAdd)
        }

        strikeListOverlay.refresh()
        mapFragment.mapView.invalidate()

        binding.legendView.requestLayout()
        binding.timeSlider.update(result.parameters, result.history!!)
    }

    // No need for onStop cleanup - ViewModel handles it!
}
```

## Step-by-Step Migration Guide

### Step 1: Update Activity to Use ViewModels

Add required imports:
```kotlin
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.blitzortung.android.app.viewmodel.MainViewModel
```

### Step 2: Replace Handler Injections

**Remove:**
```kotlin
@set:Inject
internal lateinit var locationHandler: LocationHandler

@set:Inject
internal lateinit var alertHandler: AlertHandler

@set:Inject
internal lateinit var dataHandler: MainDataHandler
```

**Add:**
```kotlin
@set:Inject
internal lateinit var viewModelFactory: ViewModelProvider.Factory

private val viewModel: MainViewModel by viewModels { viewModelFactory }
```

### Step 3: Replace Callback Consumers with Flow Collectors

**Remove:**
```kotlin
private val dataEventConsumer: (DataEvent) -> Unit = { event ->
    when (event) {
        is RequestStartedEvent -> { /* ... */ }
        is ResultEvent -> { /* ... */ }
        is StatusEvent -> { /* ... */ }
    }
}

override fun onStart() {
    super.onStart()
    dataHandler.requestUpdates(dataEventConsumer)
}

override fun onStop() {
    super.onStop()
    dataHandler.removeUpdates(dataEventConsumer)
}
```

**Add:**
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ... setup code ...
    observeViewModel()
}

private fun observeViewModel() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Multiple parallel collectors
            launch {
                viewModel.dataEvents.collect { event ->
                    when (event) {
                        is RequestStartedEvent -> { /* ... */ }
                        is ResultEvent -> { /* ... */ }
                        is StatusEvent -> { /* ... */ }
                        null -> { /* initial state */ }
                    }
                }
            }
        }
    }
}
```

### Step 4: Update Data Operations

**Before:**
```kotlin
dataHandler.updateData()
dataHandler.goRealtime()
dataHandler.setPosition(position)
dataHandler.start()
dataHandler.stop()
```

**After:**
```kotlin
viewModel.updateData()
viewModel.goRealtime()
viewModel.setPosition(position)
viewModel.start()
viewModel.stop()
```

### Step 5: Update HistoryController (if needed)

The `HistoryController` currently takes `MainDataHandler` as a parameter. You have two options:

**Option A: Keep direct handler access (temporary)**
```kotlin
// Keep the handler injection temporarily for HistoryController
@set:Inject
internal lateinit var dataHandler: MainDataHandler

historyController = HistoryController(binding, buttonColumnHandler, dataHandler)
```

**Option B: Update HistoryController to use ViewModel**
```kotlin
// Modify HistoryController constructor
class HistoryController(
    binding: MainBinding,
    buttonColumnHandler: ButtonColumnHandler<ImageButton, ButtonGroup>,
    private val viewModel: MainViewModel  // Changed from MainDataHandler
) {
    fun goRealtime() {
        if (viewModel.goRealtime()) {  // Changed from dataHandler
            viewModel.restart()
        }
    }

    // ... other methods updated similarly
}

// In Main activity:
historyController = HistoryController(binding, buttonColumnHandler, viewModel)
```

## Fragment Example: MapFragment Migration

### BEFORE: Direct Preference Access
```kotlin
class MapFragment : Fragment() {
    private lateinit var mPrefs: SharedPreferences

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.registerOnSharedPreferenceChangeListener(this)

        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Restore map state from SharedPreferences
        val zoomLevel = mPrefs.getFloat(PREFS_ZOOM_LEVEL_DOUBLE, 3.0f)
        mapView.controller.setZoom(zoomLevel.toDouble())

        val latitudeString = mPrefs.getString(PREFS_LATITUDE_STRING, null)
        val longitudeString = mPrefs.getString(PREFS_LONGITUDE_STRING, null)
        if (latitudeString != null && longitudeString != null) {
            val latitude = latitudeString.toDouble()
            val longitude = longitudeString.toDouble()
            mapView.setExpectedCenter(GeoPoint(latitude, longitude))
        }
    }

    override fun onPause() {
        super.onPause()
        // Save map state to SharedPreferences
        mPrefs.edit {
            putFloat(PREFS_ZOOM_LEVEL_DOUBLE, mapView.zoomLevelDouble.toFloat())
            putString(PREFS_LATITUDE_STRING, mapView.mapCenter.latitude.toString())
            putString(PREFS_LONGITUDE_STRING, mapView.mapCenter.longitude.toString())
        }
    }
}
```

### AFTER: ViewModel-Based Approach
```kotlin
class MapFragment : Fragment() {
    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: MapViewModel by viewModels { viewModelFactory }

    lateinit var mapView: OwnMapView
        private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inject dependencies
        AndroidSupportInjection.inject(this)

        // Set up map listeners
        setupMapListeners()

        // Observe ViewModel
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Restore zoom level
                launch {
                    viewModel.zoomLevel.collect { zoom ->
                        if (mapView.zoomLevelDouble != zoom) {
                            mapView.controller.setZoom(zoom)
                        }
                    }
                }

                // Restore center position
                launch {
                    viewModel.centerPosition.collect { position ->
                        position?.let {
                            if (mapView.mapCenter != it) {
                                mapView.setExpectedCenter(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupMapListeners() {
        // Save zoom changes to ViewModel
        mapView.addMapListener(object : org.osmdroid.events.MapListener {
            override fun onZoom(event: ZoomEvent?): Boolean {
                event?.let {
                    viewModel.updateZoomLevel(it.zoomLevel)
                }
                return true
            }

            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                // Save center position periodically
                viewModel.updateCenterPosition(mapView.mapCenter as GeoPoint)
                return true
            }
        })
    }

    override fun onPause() {
        super.onPause()
        // Save final state to ViewModel (will persist across config changes)
        viewModel.saveMapState(
            mapView.zoomLevelDouble,
            mapView.mapCenter as GeoPoint
        )
    }
}
```

## Settings Example: Using SettingsViewModel

### BEFORE: Direct SharedPreferences Access
```kotlin
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Direct preference access
        val dataSourcePref = findPreference<ListPreference>("data_source")
        dataSourcePref?.setOnPreferenceChangeListener { _, newValue ->
            // Handle change
            true
        }
    }
}
```

### AFTER: ViewModel-Based Approach
```kotlin
class SettingsFragment : PreferenceFragmentCompat() {
    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: SettingsViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.preferenceChanged.collect { key ->
                    key?.let { handlePreferenceChange(it) }
                }
            }
        }
    }

    private fun handlePreferenceChange(key: PreferenceKey) {
        when (key) {
            PreferenceKey.DATA_SOURCE -> {
                // React to data source change
                val newValue = viewModel.getStringPreference(key, "RPC")
                // Update UI or trigger actions
            }
            PreferenceKey.QUERY_PERIOD -> {
                // React to period change
                val newPeriod = viewModel.getIntPreference(key, 60)
                // Update UI or trigger actions
            }
            else -> {
                // Handle other preferences
            }
        }

        // Clear the event after handling
        viewModel.clearPreferenceChange()
    }
}
```

## Benefits of Migration

### 1. **Lifecycle Awareness**
- No manual registration/cleanup needed
- Survives configuration changes (rotation, etc.)
- Automatic cleanup when Activity/Fragment is destroyed

### 2. **Testability**
```kotlin
class MainViewModelTest {
    @Test
    fun `updateData triggers loading state`() = runTest {
        val viewModel = MainViewModel(repository, locationRepo, alertRepo)

        viewModel.updateData()

        viewModel.isLoading.test {
            assertThat(awaitItem()).isTrue()
        }
    }
}
```

### 3. **State Preservation**
```kotlin
// Before: Lost on rotation
private var currentResult: ResultEvent? = null

// After: Survives rotation automatically
viewModel.currentResult.value // Still available after rotation!
```

### 4. **Reactive UI Updates**
```kotlin
// Before: Imperative callbacks
dataHandler.requestUpdates { event ->
    when (event) {
        is ResultEvent -> updateUI(event)
    }
}

// After: Declarative Flow collection
viewModel.currentResult.collect { result ->
    result?.let { updateUI(it) }
}
```

### 5. **Centralized State**
```kotlin
// All UI state in one place
viewModel.isLoading.value
viewModel.hasError.value
viewModel.currentResult.value
viewModel.clearDataRequested.value
```

## Common Patterns

### Pattern 1: One-Shot Events
```kotlin
// In ViewModel
private val _showToast = Channel<String>()
val showToast = _showToast.receiveAsFlow()

fun triggerToast(message: String) {
    viewModelScope.launch {
        _showToast.send(message)
    }
}

// In Activity
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.showToast.collect { message ->
            Toast.makeText(this@Main, message, Toast.LENGTH_SHORT).show()
        }
    }
}
```

### Pattern 2: Combining Multiple Flows
```kotlin
// In ViewModel
val uiState: StateFlow<UiState> = combine(
    isLoading,
    hasError,
    currentResult
) { loading, error, result ->
    UiState(loading, error, result)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

// In Activity
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}
```

### Pattern 3: Conditional Collection
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.dataEvents
            .filterNotNull()
            .filterIsInstance<ResultEvent>()
            .filter { !it.failed }
            .collect { result ->
                updateUI(result)
            }
    }
}
```

## Migration Checklist

- [ ] Add ViewModelFactory injection to Activity/Fragment
- [ ] Create ViewModel instance using `by viewModels { viewModelFactory }`
- [ ] Move handler injections to ViewModel (if not needed elsewhere)
- [ ] Replace callback consumers with Flow collectors in `observeViewModel()`
- [ ] Update all data operations to use ViewModel methods
- [ ] Remove manual registration/cleanup in onStart/onStop
- [ ] Test configuration changes (rotation)
- [ ] Write unit tests for ViewModel
- [ ] Update dependent classes (e.g., HistoryController)
- [ ] Test with existing functionality

## Gradual Migration Strategy

You can migrate gradually without breaking existing code:

1. **Keep both approaches temporarily:**
```kotlin
class Main : FragmentActivity() {
    // Old approach (keep temporarily)
    @set:Inject
    internal lateinit var dataHandler: MainDataHandler

    // New approach
    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory
    private val viewModel: MainViewModel by viewModels { viewModelFactory }

    // Use ViewModel for new features
    // Use handlers for existing code that depends on them
}
```

2. **Migrate screen by screen:**
   - Start with Settings (simplest)
   - Then Map (medium complexity)
   - Finally Main Activity (most complex)

3. **Remove handler injections only when all dependencies are updated**

This allows you to migrate incrementally while keeping the app functional!
