package org.blitzortung.android.data.provider.standard

import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataBuilderTest {
    private val uut = DataBuilder()

    @Test
    fun createClusterWithoutShapeKeyYieldsAnEmptyShape() {
        val jsonObject =
            JSONObject()
                .put("id", 3L)
                .put("timestamp", "2026-10-01 20:50:00.000000000")
                .put("interval_seconds", 600)
                .put("strike_count", 1)
                .put("area", JSONObject.NULL)

        val cluster = uut.createCluster(jsonObject)

        assertThat(cluster.shape).isEmpty()
    }
}
