package org.blitzortung.android.app // Or your actual test package

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(Main::class.java)

    private lateinit var uiDevice: androidx.test.uiautomator.UiDevice
    private val dialogTimeout = 5000L // 5 seconds

    @Before
    fun setUp() {
        // Initialize UiDevice instance
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun mainActivity_isLaunched_andMapViewIsDisplayed() {
        waitForAndAcceptLocationPermissionDialog()

        waitForAndAcceptQuickSettingsDialog()

        // Check if the map view is displayed using Espresso
        onView(withId(R.id.map_view)).check(matches(isDisplayed()))
    }

    private fun waitForAndAcceptQuickSettingsDialog() {
        // Wait for the "Quick Settings" dialog to appear and find the "OK" button.
        // This is based on the assumption that the "Quick Settings" dialog appears after location permission.
        val quickSettingsOkButton = uiDevice.wait(
            Until.findObject(By.text("OK")), // Adjust "OK" if the button text is different
            dialogTimeout // Reuse timeout or define a new one for this dialog
        )

        // Click the "OK" button on the Quick Settings dialog
        quickSettingsOkButton?.click()
    }

    private fun waitForAndAcceptLocationPermissionDialog() {
        // Define the text pattern for the "allow" button on the location permission dialog.
        // This covers "While using the app" and "Allow only while using the app".
        val allowButtonTextPattern = Pattern.compile(
            "while using the app|allow only while using the app",
            Pattern.CASE_INSENSITIVE
        )

        // Wait for the permission dialog's "allow" button to appear and find it.
        val allowButton = uiDevice.wait(
            Until.findObject(By.text(allowButtonTextPattern)),
            dialogTimeout
        )

        // Verify that the button was found
        Assert.assertNotNull(
            "Location permission dialog with 'Allow' button not found within ${dialogTimeout}ms. " +
                    "Permissions might have been already granted or the dialog has different text.",
            allowButton
        )

        // Click the "allow" button
        allowButton.click()

        // Optional: Wait for the dialog to disappear to ensure the action is processed.
        uiDevice.wait(Until.gone(By.text(allowButtonTextPattern)), 2000L)
    }
}
