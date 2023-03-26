package org.blitzortung.android.app.components

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VersionComponentTest {

    private lateinit var versionComponent: VersionComponent

    @MockK
    private lateinit var buildVersion: BuildVersion

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        every { buildVersion.majorVersion } answers { MAJOR_VERSION }
        every { buildVersion.minorVersion } answers { MINOR_VERSION }
        every { buildVersion.patchVersion } answers { PATCH_VERSION }
        every { buildVersion.versionCode } answers { VERSION_CODE }
        versionComponent = VersionComponent(ApplicationProvider.getApplicationContext(), buildVersion)
    }

    @Test
    fun configuredVersionCodeShouldBeMinusOneWhenUndefied() {
        assertThat(versionComponent.configuredVersionCode).isEqualTo(-1)
    }

    @Test
    fun stateShouldBeFirstRunWhenConfiguredVersionIsUndefined() {
        assertThat(versionComponent.state).isEqualTo(VersionComponent.State.FIRST_RUN)
    }

    @Test
    fun shouldReturnNoUpdateStateWhenCalledNextTime() {
        versionComponent = VersionComponent(RuntimeEnvironment.application, buildVersion)
        assertThat(versionComponent.configuredVersionCode).isEqualTo(VERSION_CODE)
        assertThat(versionComponent.state).isEqualTo(VersionComponent.State.NO_UPDATE)
    }

    @Test
    fun shouldReturnUpdatedStateWhenCalledFirstTimeAfterMajorVersionChange() {
        val context = RuntimeEnvironment.getApplication()

        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        preferences.edit().putInt(VersionComponent.CONFIGURED_MAJOR_VERSION, MAJOR_VERSION - 1).apply()

        versionComponent = VersionComponent(context, buildVersion)
        assertThat(versionComponent.configuredMajorVersion).isEqualTo(MAJOR_VERSION - 1)
        assertThat(versionComponent.state).isEqualTo(VersionComponent.State.FIRST_RUN_AFTER_UPDATE)
    }

    @Test
    fun shouldReturnUpdatedStateWhenCalledFirstTimeAfterMinorVersionChange() {
        val context = RuntimeEnvironment.getApplication()

        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        preferences.edit().putInt(VersionComponent.CONFIGURED_MINOR_VERSION, MINOR_VERSION - 1).apply()

        versionComponent = VersionComponent(context, buildVersion)
        assertThat(versionComponent.configuredMinorVersion).isEqualTo(MINOR_VERSION - 1)
        assertThat(versionComponent.state).isEqualTo(VersionComponent.State.FIRST_RUN_AFTER_UPDATE)
    }

    companion object {
        val VERSION_CODE = 100
        val MAJOR_VERSION = 1
        val MINOR_VERSION = 2
        val PATCH_VERSION = 3
    }
}

class BuildVersionTest {
    private lateinit var buildVersion: BuildVersion

    @Before
    fun setUp() {
        buildVersion = BuildVersion()
    }

    @Test
    fun returnsMajorVersion() {
        assertThat(buildVersion.majorVersion).isGreaterThan(0)
    }

    @Test
    fun returnsMinorVersion() {
        assertThat(buildVersion.majorVersion).isGreaterThanOrEqualTo(0)
    }

    @Test
    fun returnsPatchVersion() {
        assertThat(buildVersion.majorVersion).isGreaterThanOrEqualTo(-1)
    }
}

