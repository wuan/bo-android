# ViewModel Migration Quick Reference

## Quick Comparison Chart

| **Aspect** | **Before (Handlers)** | **After (ViewModels)** |
|------------|----------------------|------------------------|
| **Dependency Injection** | `@set:Inject lateinit var dataHandler: MainDataHandler` | `@set:Inject lateinit var viewModelFactory: ViewModelProvider.Factory`<br>`private val viewModel: MainViewModel by viewModels { viewModelFactory }` |
| **Data Updates** | Callback-based: `dataHandler.requestUpdates(consumer)` | Flow-based: `viewModel.dataEvents.collect { }` |
| **Lifecycle Management** | Manual: `onStart()` → register, `onStop()` → unregister | Automatic: Lifecycle-aware collection |
| **Configuration Changes** | Data lost on rotation (unless saved) | Data preserved automatically |
| **State Access** | Direct: `dataHandler.parameters` | Reactive: `viewModel.getParameters()` |
| **Testing** | Hard to mock callbacks | Easy to test with coroutine test helpers |

## Essential Code Snippets

### 1. Activity Setup

```kotlin
class Main : FragmentActivity() {
    // Inject ViewModelFactory
    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    // Create ViewModel
    private val viewModel: MainViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeViewModel()
        viewModel.start()
    }
}
```

### 2. Observing Data (The Core Pattern)

```kotlin
private fun observeViewModel() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // Loading state
            launch {
                viewModel.isLoading.collect { isLoading ->
                    if (isLoading) {
                        statusComponent.startProgress()
                    } else {
                        statusComponent.stopProgress()
                    }
                }
            }

            // Error state
            launch {
                viewModel.hasError.collect { hasError ->
                    statusComponent.indicateError(hasError)
                }
            }

            // Data results
            launch {
                viewModel.currentResult.collect { result ->
                    result?.let { updateStrikeOverlay(it) }
                }
            }

            // Location updates
            launch {
                viewModel.locationEvents.collect { event ->
                    event?.let { updateLocationOverlay(it.location) }
                }
            }

            // Alert events
            launch {
                viewModel.alertEvents.collect { event ->
                    event?.let { handleAlert(it) }
                }
            }
        }
    }
}
```

### 3. Fragment Setup

```kotlin
class MapFragment : Fragment() {
    @set:Inject
    internal lateinit var viewModelFactory: ViewModelProvider.Factory

    private val viewModel: MapViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidSupportInjection.inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.zoomLevel.collect { zoom ->
                        mapView.controller.setZoom(zoom)
                    }
                }

                launch {
                    viewModel.centerPosition.collect { position ->
                        position?.let { mapView.setExpectedCenter(it) }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveMapState(
            mapView.zoomLevelDouble,
            mapView.mapCenter as GeoPoint
        )
    }
}
```

## Common Operations Translation Table

### Data Operations

| **Before** | **After** |
|------------|-----------|
| `dataHandler.updateData()` | `viewModel.updateData()` |
| `dataHandler.goRealtime()` | `viewModel.goRealtime()` |
| `dataHandler.setPosition(pos)` | `viewModel.setPosition(pos)` |
| `dataHandler.start()` | `viewModel.start()` |
| `dataHandler.stop()` | `viewModel.stop()` |
| `dataHandler.restart()` | `viewModel.restart()` |
| `dataHandler.startAnimation()` | `viewModel.startAnimation()` |
| `dataHandler.toggleExtendedMode()` | `viewModel.toggleExtendedMode()` |
| `dataHandler.parameters` | `viewModel.getParameters()` |
| `dataHandler.intervalDuration` | `viewModel.getIntervalDuration()` |
| `dataHandler.isRealtime` | `viewModel.isRealtime()` |
| `dataHandler.historySteps()` | `viewModel.historySteps()` |

### Location Operations

| **Before** | **After** |
|------------|-----------|
| `locationHandler.location` | `viewModel.locationEvents.value?.location` or `locationRepository.getCurrentLocation()` |
| `locationHandler.enableBackgroundMode()` | `viewModel.enableBackgroundLocation()` |
| `locationHandler.disableBackgroundMode()` | `viewModel.disableBackgroundLocation()` |

### Alert Operations

| **Before** | **After** |
|------------|-----------|
| `alertHandler.alertParameters` | `viewModel.alertEvents.value` or via AlertRepository |
| `alertHandler.requestUpdates(consumer)` | `viewModel.alertEvents.collect { }` |

## Callback → Flow Conversion Examples

### Example 1: Simple Event Handling

**Before:**
```kotlin
private val dataEventConsumer: (DataEvent) -> Unit = { event ->
    when (event) {
        is RequestStartedEvent -> handleRequestStarted()
        is ResultEvent -> handleResult(event)
        is StatusEvent -> handleStatus(event)
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

**After:**
```kotlin
private fun observeViewModel() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.dataEvents.collect { event ->
                when (event) {
                    is RequestStartedEvent -> handleRequestStarted()
                    is ResultEvent -> handleResult(event)
                    is StatusEvent -> handleStatus(event)
                    null -> {} // Initial state
                }
            }
        }
    }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    observeViewModel()
}
```

### Example 2: Multiple Parallel Observers

**Before:**
```kotlin
private val dataConsumer: (DataEvent) -> Unit = { /* ... */ }
private val locationConsumer: (LocationEvent) -> Unit = { /* ... */ }
private val alertConsumer: (AlertEvent) -> Unit = { /* ... */ }

override fun onStart() {
    super.onStart()
    dataHandler.requestUpdates(dataConsumer)
    locationHandler.requestUpdates(locationConsumer)
    alertHandler.requestUpdates(alertConsumer)
}

override fun onStop() {
    super.onStop()
    dataHandler.removeUpdates(dataConsumer)
    locationHandler.removeUpdates(locationConsumer)
    alertHandler.removeUpdates(alertConsumer)
}
```

**After:**
```kotlin
private fun observeViewModel() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            // All run in parallel automatically!
            launch { viewModel.dataEvents.collect { /* ... */ } }
            launch { viewModel.locationEvents.collect { /* ... */ } }
            launch { viewModel.alertEvents.collect { /* ... */ } }
        }
    }
}

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    observeViewModel()
}
// No onStop cleanup needed!
```

### Example 3: Filtering Events

**Before:**
```kotlin
private val dataConsumer: (DataEvent) -> Unit = { event ->
    if (event is ResultEvent && !event.failed) {
        processSuccessfulResult(event)
    }
}
```

**After:**
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.dataEvents
            .filterNotNull()
            .filterIsInstance<ResultEvent>()
            .filter { !it.failed }
            .collect { result ->
                processSuccessfulResult(result)
            }
    }
}
```

## UI State Patterns

### Pattern 1: Show/Hide Progress

**Before:**
```kotlin
private val dataConsumer: (DataEvent) -> Unit = { event ->
    when (event) {
        is RequestStartedEvent -> progressBar.visibility = View.VISIBLE
        is ResultEvent -> progressBar.visibility = View.GONE
    }
}
```

**After:**
```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.isLoading.collect { isLoading ->
            progressBar.isVisible = isLoading
        }
    }
}
```

### Pattern 2: Update UI from Multiple States

**Before:**
```kotlin
private var isLoading = false
private var hasError = false
private var data: ResultEvent? = null

private fun updateUI() {
    when {
        isLoading -> showLoading()
        hasError -> showError()
        data != null -> showData(data!!)
        else -> showEmpty()
    }
}
```

**After:**
```kotlin
// In ViewModel
data class UiState(
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val data: ResultEvent? = null
)

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
            when {
                state.isLoading -> showLoading()
                state.hasError -> showError()
                state.data != null -> showData(state.data)
                else -> showEmpty()
            }
        }
    }
}
```

### Pattern 3: One-Time Events (like Toasts or Navigation)

```kotlin
// In ViewModel
private val _navigationEvent = Channel<NavigationDestination>()
val navigationEvent = _navigationEvent.receiveAsFlow()

fun navigateToSettings() {
    viewModelScope.launch {
        _navigationEvent.send(NavigationDestination.Settings)
    }
}

// In Activity
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.navigationEvent.collect { destination ->
            when (destination) {
                NavigationDestination.Settings -> {
                    startActivity(Intent(this@Main, SettingsActivity::class.java))
                }
            }
        }
    }
}
```

## Testing Examples

### Testing with ViewModels

**Before (Testing with Handlers):**
```kotlin
@Test
fun `data update shows loading`() {
    val handler = mock<MainDataHandler>()
    val consumer = slot<(DataEvent) -> Unit>()

    every { handler.requestUpdates(capture(consumer)) } returns Unit

    val activity = createActivity()

    // Trigger callback
    consumer.captured(RequestStartedEvent())

    // Assert UI state
    assertThat(activity.isLoading).isTrue()
}
```

**After (Testing ViewModels):**
```kotlin
@Test
fun `data update shows loading`() = runTest {
    val repository = mock<StrikeDataRepository>()
    every { repository.observeDataEvents() } returns flowOf(RequestStartedEvent())

    val viewModel = MainViewModel(repository, locationRepo, alertRepo)

    viewModel.isLoading.test {
        assertThat(awaitItem()).isTrue()
    }
}
```

## Troubleshooting

### Problem: StateFlow not updating

**Solution:** Make sure you're collecting in the right lifecycle state:
```kotlin
// ✅ Correct - only collects when STARTED or RESUMED
repeatOnLifecycle(Lifecycle.State.STARTED) {
    viewModel.dataEvents.collect { }
}

// ❌ Wrong - collects even when app is in background
lifecycleScope.launch {
    viewModel.dataEvents.collect { }
}
```

### Problem: Multiple collections triggering

**Solution:** Use `StateFlow` for state, `SharedFlow` or `Channel` for events:
```kotlin
// For state (UI always shows latest)
val isLoading: StateFlow<Boolean>

// For one-time events (like navigation or toasts)
val showToast: SharedFlow<String>
// or
val navigationEvent: Flow<NavigationEvent> = channel.receiveAsFlow()
```

### Problem: Memory leaks

**Solution:** Always use `viewLifecycleOwner` in Fragments:
```kotlin
// ✅ Correct in Fragment
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { }
}

// ❌ Wrong in Fragment - may leak
lifecycleScope.launch { }
```

## Required Imports

Add these imports for ViewModel migration:

```kotlin
// ViewModel
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel

// Lifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

// Coroutines & Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

// Your ViewModels
import org.blitzortung.android.app.viewmodel.MainViewModel
import org.blitzortung.android.app.viewmodel.MapViewModel
import org.blitzortung.android.app.viewmodel.SettingsViewModel
```

## Migration Priority

Suggested order for migrating screens:

1. **SettingsViewModel** (Easiest)
   - Minimal UI updates
   - Simple preference management
   - Good learning opportunity

2. **MapViewModel** (Medium)
   - State preservation
   - Map listener integration
   - Configuration change handling

3. **MainViewModel** (Complex)
   - Multiple data streams
   - Complex UI updates
   - Coordination between multiple repositories

## Final Tips

✅ **DO:**
- Migrate one screen at a time
- Test thoroughly after each migration
- Keep both approaches temporarily during transition
- Write tests for ViewModels
- Use `repeatOnLifecycle` for collections

❌ **DON'T:**
- Try to migrate everything at once
- Forget to inject ViewModelFactory
- Collect flows without lifecycle awareness
- Remove handler injections before updating all dependencies
- Mix callbacks and Flow collections for the same data

---

**Need help?** Check the full migration guide in `VIEWMODEL_MIGRATION_EXAMPLE.md`
