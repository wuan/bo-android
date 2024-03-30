package org.blitzortung.android.data.cache

import io.mockk.MockKAnnotations
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.Flags
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.TimeInterval
import org.blitzortung.android.data.beans.RasterElement
import org.blitzortung.android.data.provider.result.ResultEvent
import org.junit.Before
import org.junit.Test

class DataCacheTest {

    private val parameters = Parameters(interval = TimeInterval(offset = 30))

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
        val dataEvent = ResultEvent(parameters = parameters, flags = Flags())
        uut.put(parameters, dataEvent)

        val result = uut.get(parameters)

        assertThat(result).isEqualTo(dataEvent)
    }

    @Test
    fun cacheClear() {
        val dataEvent = ResultEvent(parameters = parameters, flags = Flags())
        uut.put(parameters, dataEvent)
        uut.clear()

        val result = uut.get(parameters)

        assertThat(result).isNull()
    }

    @Test
    fun cachePutOutdated() {
        val dataEvent = ResultEvent(parameters = parameters, flags = Flags())
        uut.cache[parameters] = Timestamped(dataEvent, System.currentTimeMillis() - DataCache.DEFAULT_EXPIRY_TIME - 1)

        val result = uut.get(parameters)

        assertThat(result).isNull()
    }

    @Test
    fun cacheSizeSimple() {
        val strikes = listOf(
            RasterElement(0, 1.0, 2.0, 3),
            RasterElement(2, 3.0, 4.0, 2),
            RasterElement(4, -3.0, -4.0, 1),
        )
        val dataEvent = ResultEvent(parameters = parameters, flags = Flags(), strikes = strikes)

        uut.put(parameters, dataEvent)
        uut.put(parameters.copy(interval = TimeInterval(offset = 60)), ResultEvent(parameters = parameters, flags = Flags()))

        val result = uut.calculateTotalSize()

        assertThat(result).isEqualTo(CacheSize(2, 3))
    }
}