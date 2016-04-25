package org.blitzortung.android.protocol

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.KotlinAssertions.assertThat
import org.junit.Before
import org.junit.Test

class ConsumerContainerTest {

    lateinit var testConsumerContainer: TestConsumerContainer

    @Before
    fun setUp() {
        testConsumerContainer = TestConsumerContainer()
    }

    @Test
    fun emptyContainerShouldBeEmptyAndHaveSizeZero() {
        assertThat(testConsumerContainer.isEmpty).isTrue()
        assertThat(testConsumerContainer.size).isEqualTo(0)
    }

    @Test
    fun containerWithConsumerShouldBeNotEmptyAndHaveSizeOne() {
        testConsumerContainer.addConsumer { }

        assertThat(testConsumerContainer.isEmpty).isFalse()
        assertThat(testConsumerContainer.size).isEqualTo(1)
    }

    @Test
    fun consumerShoudReceivePayloadBroadcast() {
        var result: String? = null
        testConsumerContainer.addConsumer { string -> result = string }

        testConsumerContainer.broadcast("foo")
        assertThat(result).isEqualTo("foo")
    }

    @Test
    fun addingNullConsumerShouldThrow() {
        assertThatThrownBy {
            testConsumerContainer.addConsumer(null)
        }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("consumer may not be null")
    }

    @Test
    fun addedConsumerShouldReceiveNoDataWithNoCurrentPayloadSet() {
        var result: String? = null
        testConsumerContainer.addConsumer { string -> result = string }

        assertThat(result).isNull()
    }

    @Test
    fun addedConsumerShouldReceiveCurrentPayload() {
        testConsumerContainer.storeAndBroadcast("bar")

        var result: String? = null
        testConsumerContainer.addConsumer { string -> result = string }

        assertThat(result).isEqualTo("bar")
    }

    @Test
    fun addingFirstConsumerShouldBeRecorded() {
        assertThat(testConsumerContainer.firstConsumersAdded).isEqualTo(0)

        testConsumerContainer.addConsumer { }

        assertThat(testConsumerContainer.firstConsumersAdded).isEqualTo(1)

        testConsumerContainer.addConsumer { }

        assertThat(testConsumerContainer.firstConsumersAdded).isEqualTo(1)
        assertThat(testConsumerContainer.size).isEqualTo(2)
    }

    @Test
    fun removingLastConsumerShouldBeRecorded() {
        val consumer1: (String) -> Unit = { text -> println(text) }
        val consumer2: (String) -> Unit = { text -> println(text) }
        testConsumerContainer.addConsumer(consumer1)
        testConsumerContainer.addConsumer(consumer2)
        assertThat(testConsumerContainer.size).isEqualTo(2)

        testConsumerContainer.removeConsumer(consumer2)
        assertThat(testConsumerContainer.lastConsumersRemoved).isEqualTo(0)

        testConsumerContainer.removeConsumer(consumer1)
        assertThat(testConsumerContainer.lastConsumersRemoved).isEqualTo(1)
        assertThat(testConsumerContainer.isEmpty).isTrue()
    }
}

class TestConsumerContainer : ConsumerContainer<String>() {

    var firstConsumersAdded = 0
        private set

    var lastConsumersRemoved = 0
        private set

    override fun addedFirstConsumer() {
        synchronized(firstConsumersAdded) {
            firstConsumersAdded++
        }
    }

    override fun removedLastConsumer() {
        synchronized(lastConsumersRemoved) {
            lastConsumersRemoved++
        }
    }
}