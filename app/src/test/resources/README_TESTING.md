# Testing Guide for Blitzortung Lightning Monitor

This guide demonstrates how to use the newly added testing libraries in this project.

## Available Testing Libraries

### Unit Testing
- **JUnit 4.13.2** - Test framework
- **AssertJ 3.27.4** - Fluent assertions
- **MockK 1.14.5** - Mocking framework for Kotlin
- **Robolectric 4.15.1** - Android framework testing without emulator

### Coroutines Testing
- **kotlinx-coroutines-test 1.9.0** - Test coroutines with virtual time
- **Turbine 1.2.0** - Flow testing made easy

### AndroidX Testing
- **androidx.test:core** - Core testing utilities
- **androidx.test:rules** - Test rules (ActivityScenarioRule, etc.)
- **androidx.arch.core:core-testing** - LiveData/ViewModel testing
- **fragment-testing** - Fragment testing in isolation

### Instrumented Testing
- **Espresso** - UI testing framework
- **UIAutomator** - System-level UI testing
- **MockK Android** - Mocking for instrumented tests

## Example Tests

### 1. Testing Coroutines with StandardTestDispatcher

```kotlin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineExampleTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun `test coroutine with virtual time`() = runTest(testDispatcher) {
        // Given
        val repository = MyRepository(testDispatcher)

        // When
        val job = launch { repository.fetchData() }
        advanceUntilIdle() // Fast-forward virtual time

        // Then
        assertThat(repository.data).isNotNull()
    }
}
```

### 2. Testing Flows with Turbine

```kotlin
import app.cash.turbine.test
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.assertj.core.api.Assertions.assertThat

class FlowExampleTest {

    @Test
    fun `test flow emissions with Turbine`() = runTest {
        // Given
        val flow = flow {
            emit(1)
            emit(2)
            emit(3)
        }

        // When/Then
        flow.test {
            assertThat(awaitItem()).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(2)
            assertThat(awaitItem()).isEqualTo(3)
            awaitComplete()
        }
    }

    @Test
    fun `test flow with multiple collectors`() = runTest {
        val dataHandler = MainDataHandler(...)

        dataHandler.dataEvents.test {
            // Trigger data update
            dataHandler.updateData()

            // Verify events
            val event = awaitItem()
            assertThat(event).isInstanceOf(ResultEvent::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 3. Testing with MockK

```kotlin
import io.mockk.*
import org.junit.Before
import org.junit.Test

class MockKExampleTest {

    private lateinit var dataProvider: DataProvider
    private lateinit var handler: MainDataHandler

    @Before
    fun setup() {
        dataProvider = mockk()
        handler = MainDataHandler(dataProvider)
    }

    @Test
    fun `verify method was called`() {
        // Given
        every { dataProvider.fetchStrikes() } returns emptyList()

        // When
        handler.updateData()

        // Then
        verify { dataProvider.fetchStrikes() }
    }

    @Test
    fun `test with captured arguments`() {
        // Given
        val paramSlot = slot<Parameters>()
        every { dataProvider.fetch(capture(paramSlot)) } returns mockk()

        // When
        handler.updateData()

        // Then
        assertThat(paramSlot.captured.intervalDuration).isEqualTo(60)
    }
}
```

### 4. Testing LiveData with InstantTaskExecutorRule

```kotlin
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Rule
import org.junit.Test

class LiveDataExampleTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Test
    fun `test LiveData emissions`() {
        // Given
        val viewModel = MyViewModel()

        // When
        viewModel.loadData()

        // Then
        assertThat(viewModel.data.value).isNotNull()
    }
}
```

### 5. Testing with Robolectric (Android components without emulator)

```kotlin
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RobolectricExampleTest {

    @Test
    fun `test Android component`() {
        // Given
        val context = ApplicationProvider.getApplicationContext<Context>()

        // When
        val service = MyService(context)
        service.start()

        // Then
        assertThat(service.isRunning).isTrue()
    }
}
```

### 6. Testing Fragments

```kotlin
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FragmentExampleTest {

    @Test
    fun `test fragment displays data`() {
        // Given
        val scenario = launchFragmentInContainer<MapFragment>()

        // When
        scenario.onFragment { fragment ->
            fragment.updateStrikes(listOf(mockStrike))
        }

        // Then
        // Verify UI state
    }
}
```

### 7. Testing Espresso UI

```kotlin
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EspressoExampleTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(Main::class.java)

    @Test
    fun clickButton_showsDialog() {
        // When
        onView(withId(R.id.settings_button)).perform(click())

        // Then
        onView(withText("Settings")).check(matches(isDisplayed()))
    }
}
```

### 8. Testing with Test Rules

```kotlin
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

class TestRulesExampleTest {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    @Test
    fun `test location with granted permission`() {
        // Test code that requires location permission
    }
}
```

## Best Practices

### 1. Use runTest for Coroutine Tests
```kotlin
@Test
fun myTest() = runTest {
    // Coroutine code here - uses virtual time
}
```

### 2. Use Turbine for Flow Testing
```kotlin
flow.test {
    assertThat(awaitItem()).isEqualTo(expected)
    awaitComplete()
}
```

### 3. Mock External Dependencies
```kotlin
private val mockRepository = mockk<Repository>()
every { mockRepository.getData() } returns testData
```

### 4. Clean Up After Tests
```kotlin
@After
fun tearDown() {
    unmockkAll()
}
```

### 5. Use AssertJ for Readable Assertions
```kotlin
// Instead of:
assertEquals(3, list.size)

// Use:
assertThat(list).hasSize(3)
```

## Running Tests

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific test class
./gradlew testDebugUnitTest --tests "*.MainDataHandlerTest"

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport

# Run instrumented tests
./gradlew connectedDebugAndroidTest
```

## Test Coverage Reports

After running tests with coverage:
```bash
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## Additional Resources

- [Kotlin Coroutines Testing](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)
- [Turbine Documentation](https://github.com/cashapp/turbine)
- [MockK Documentation](https://mockk.io/)
- [Robolectric](http://robolectric.org/)
- [Espresso Testing](https://developer.android.com/training/testing/espresso)
