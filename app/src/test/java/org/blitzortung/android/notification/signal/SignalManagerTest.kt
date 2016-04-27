package org.blitzortung.android.notification.signal

import android.content.Context
import android.content.SharedPreferences
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.junit.Test

class SignalManagerTest {

    private val context : Context = mock()

    private val preferences : SharedPreferences = mock()

    private val signal : NotificationSignal = mock()

    @Test
    fun signalCallShouldBeDelegatedToSignals() {
        val signalManager = SignalManager(context, preferences, listOf(signal))

        signalManager.signal()

        verify(signal).signal()
    }
}