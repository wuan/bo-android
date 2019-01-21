package org.blitzortung.android.app.components

import android.content.Context
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

    lateinit private var versionComponent: VersionComponent

    @Before
    fun setUp() {
        versionComponent = VersionComponent(RuntimeEnvironment.application)
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
    fun shouldReturnVersionCode() {
        assertThat(versionComponent.versionCode).isEqualTo(CURRENT_VERSION_CODE)
    }

    @Test
    fun shouldReturnVersionName() {
        assertThat(versionComponent.versionName).isEqualTo(CURRENT_VERSION_NAME)
    }

    @Test
    fun shouldReturnNoUpdateStateWhenCalledNextTime() {
        versionComponent = VersionComponent(RuntimeEnvironment.application)
        assertThat(versionComponent.configuredVersionCode).isEqualTo(CURRENT_VERSION_CODE)
        assertThat(versionComponent.state).isEqualTo(VersionComponent.State.NO_UPDATE)
    }

    @Test
    fun shouldReturnUpdatedStateWhenCalledFirstTimeAfterVersionChange() {
        val context = RuntimeEnvironment.application

        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        preferences.edit().putInt(VersionComponent.CONFIGURED_VERSION_CODE, 1).apply()

        versionComponent = VersionComponent(context)
        assertThat(versionComponent.configuredVersionCode).isEqualTo(1)
        assertThat(versionComponent.state).isEqualTo(VersionComponent.State.FIRST_RUN_AFTER_UPDATE)
    }

    companion object {
        val CURRENT_VERSION_CODE = 210
        val CURRENT_VERSION_NAME = "2.0.0"
    }
}