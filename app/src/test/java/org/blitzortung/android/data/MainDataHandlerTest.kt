package org.blitzortung.android.data

import android.content.Context
import android.os.Handler
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.cache.DataCache
import org.blitzortung.android.data.provider.DataProviderFactory
import org.blitzortung.android.data.provider.LocalData
import org.blitzortung.android.util.Period
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class MainDataHandlerTest {

    @MockK
    private lateinit var dataProviderFactory: DataProviderFactory

    @MockK
    private lateinit var handler: Handler

    @MockK
    private lateinit var localData: LocalData

    @MockK
    private lateinit var period: Period

    private lateinit var uut: MainDataHandler

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        uut = MainDataHandler(context, dataProviderFactory, preferences, handler, DataCache(), localData, period)
    }

    @Test
    fun blabla() {
        assertThat(uut.isRealtime).isTrue
    }
}