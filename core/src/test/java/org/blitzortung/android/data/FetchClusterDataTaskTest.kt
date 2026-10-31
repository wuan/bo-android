package org.blitzortung.android.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.data.provider.DataProviderType
import org.blitzortung.android.data.provider.data.DataProvider
import org.blitzortung.android.data.provider.result.ClusterReceived
import org.blitzortung.android.data.provider.result.DataReceived
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FetchClusterDataTaskTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun deliversSuccessfulResult() {
        val parameters = ClusterParameters(intervalCount = 3)
        val expected = ClusterReceived(parameters = parameters, referenceTime = 123L)
        var received: ClusterReceived? = null

        FetchClusterDataTask(FakeClusterDataProvider(result = expected), dispatcher) { received = it }
            .execute(parameters)

        assertThat(received).isEqualTo(expected)
    }

    @Test
    fun reportsFailureWhenTheProviderThrows() {
        val parameters = ClusterParameters()
        var received: ClusterReceived? = null

        FetchClusterDataTask(
            FakeClusterDataProvider(error = RuntimeException("boom")),
            dispatcher,
        ) { received = it }.execute(parameters)

        assertThat(received).isNotNull
        assertThat(received?.failed).isTrue()
        assertThat(received?.parameters).isEqualTo(parameters)
    }

    @Test
    fun cancelPreventsDelivery() {
        val parameters = ClusterParameters()
        var received: ClusterReceived? = null
        val task =
            FetchClusterDataTask(
                FakeClusterDataProvider(result = ClusterReceived(parameters = parameters)),
                dispatcher,
            ) { received = it }

        task.cancel()
        task.execute(parameters)

        assertThat(received).isNull()
    }

    private class FakeClusterDataProvider(
        private val result: ClusterReceived? = null,
        private val error: RuntimeException? = null,
    ) : DataProvider {
        override val type: DataProviderType = DataProviderType.RPC

        override val supportsClusters: Boolean = true

        override fun reset() = Unit

        override fun <T> retrieveData(retrieve: DataProvider.DataRetriever.() -> T): T {
            error?.let { throw it }
            val retriever =
                object : DataProvider.DataRetriever {
                    override fun getClusters(parameters: ClusterParameters): ClusterReceived = requireNotNull(result)

                    override fun getStrikes(
                        parameters: Parameters,
                        history: History?,
                        flags: Flags,
                    ): DataReceived = throw UnsupportedOperationException()

                    override fun getStrikesGrid(
                        parameters: Parameters,
                        history: History?,
                        flags: Flags,
                    ): DataReceived = throw UnsupportedOperationException()
                }
            return retriever.retrieve()
        }
    }
}
