package org.blitzortung.android.data.cache

import io.mockk.MockKAnnotations
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.ClusterParameters
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.beans.Cluster
import org.blitzortung.android.data.beans.GridElement
import org.blitzortung.android.data.provider.result.ClusterReceived
import org.blitzortung.android.data.provider.result.DataReceived
import org.junit.Before
import org.junit.Test

class DataCacheTest {
    private val parameters = Parameters(interval = TimeInterval(offset = 30))
    private val clusterParameters = ClusterParameters(intervalCount = 6)

    private lateinit var uut: DataCache

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)

        uut = DataCache()
    }

    @Test
    fun cacheMiss() {
        val result = uut.get(parameters)

        assertThat(result).isNull()
    }

    @Test
    fun emptySize() {
        val result = uut.calculateTotalSize()

        assertThat(result).isEqualTo(CacheSize(0, 0))
    }

    @Test
    fun cachePut() {
        val dataEvent = DataReceived(parameters = parameters, flags = Flags())
        uut.put(parameters, dataEvent)

        val result = uut.get(parameters)

        assertThat(result).isEqualTo(dataEvent)
    }

    @Test
    fun cacheClear() {
        val dataEvent = DataReceived(parameters = parameters, flags = Flags())
        uut.put(parameters, dataEvent)
        uut.clear()

        val result = uut.get(parameters)

        assertThat(result).isNull()
    }

    @Test
    fun cachePutOutdated() {
        val dataEvent = DataReceived(parameters = parameters, flags = Flags())
        uut.cache[parameters] = Timestamped(dataEvent, System.currentTimeMillis() - DataCache.DEFAULT_EXPIRY_TIME - 1)

        val result = uut.get(parameters)

        assertThat(result).isNull()
    }

    @Test
    fun cacheSizeSimple() {
        val strikes =
            listOf(
                GridElement(0, 1.0, 2.0, 3),
                GridElement(2, 3.0, 4.0, 2),
                GridElement(4, -3.0, -4.0, 1),
            )
        val dataEvent = DataReceived(parameters = parameters, flags = Flags(), strikes = strikes)

        uut.put(parameters, dataEvent)
        uut.put(
            parameters.copy(interval = TimeInterval(offset = 60)),
            DataReceived(parameters = parameters, flags = Flags()),
        )

        val result = uut.calculateTotalSize()

        assertThat(result).isEqualTo(CacheSize(2, 3))
    }

    @Test
    fun clusterCacheMiss() {
        assertThat(uut.getCluster(clusterParameters)).isNull()
    }

    @Test
    fun clusterCachePutAndGet() {
        val cluster = Cluster(1L, 1_000L, 600, 7, 1.0, listOf(11.0 to 51.0))
        val dataEvent = ClusterReceived(clusters = listOf(cluster), parameters = clusterParameters)

        uut.putCluster(clusterParameters, dataEvent)

        assertThat(uut.getCluster(clusterParameters)).isEqualTo(dataEvent)
    }

    @Test
    fun clusterCachePutOutdated() {
        val dataEvent = ClusterReceived(parameters = clusterParameters)
        uut.clusterCache[clusterParameters] =
            Timestamped(dataEvent, System.currentTimeMillis() - DataCache.CLUSTER_EXPIRY_TIME - 1)

        assertThat(uut.getCluster(clusterParameters)).isNull()
    }

    @Test
    fun clusterCacheCleared() {
        uut.putCluster(clusterParameters, ClusterReceived(parameters = clusterParameters))

        uut.clear()

        assertThat(uut.getCluster(clusterParameters)).isNull()
    }
}
