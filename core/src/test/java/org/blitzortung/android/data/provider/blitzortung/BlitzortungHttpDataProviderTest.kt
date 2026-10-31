package org.blitzortung.android.data.provider.blitzortung

import android.content.Context
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.ClusterParameters
import org.blitzortung.android.data.DataArea
import org.blitzortung.android.data.provider.result.ClusterReceived
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BlitzortungHttpDataProviderTest {
    private lateinit var uut: BlitzortungHttpDataProvider

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        val preferences = context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

        uut = BlitzortungHttpDataProvider(preferences, UrlFormatter(), MapBuilderFactory())
    }

    @Test
    fun doesNotSupportClusters() {
        assertThat(uut.supportsClusters).isFalse
    }

    @Test
    fun clusterRequestIsANoOpWithoutNetworkAccess() {
        val parameters = ClusterParameters(global = false, dataArea = DataArea(4, 9, 5))

        val result: ClusterReceived = uut.retrieveData { getClusters(parameters) }

        assertThat(result.clusters).isEmpty()
        assertThat(result.failed).isFalse
        assertThat(result.parameters).isEqualTo(parameters)
    }
}
