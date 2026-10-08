package org.blitzortung.android.app.viewmodel

import androidx.lifecycle.ViewModel
import io.mockk.every
import io.mockk.mockk
import javax.inject.Provider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Before
import org.junit.Test

class ViewModelFactoryTest {
    private lateinit var mainViewModel: MainViewModel
    private lateinit var uut: ViewModelFactory

    @Before
    fun setUp() {
        mainViewModel = mockk(relaxed = true)
        val provider =
            object : Provider<ViewModel> {
                override fun get(): ViewModel = mainViewModel
            }
        uut = ViewModelFactory(mapOf(MainViewModel::class.java to provider))
    }

    @Test
    fun createsRegisteredViewModel() {
        assertThat(uut.create(MainViewModel::class.java)).isSameAs(mainViewModel)
    }

    @Test
    fun fallsBackToAssignableViewModel() {
        assertThat(uut.create(ViewModel::class.java)).isSameAs(mainViewModel)
    }

    @Test
    fun throwsForUnknownViewModel() {
        assertThatThrownBy { uut.create(MapViewModel::class.java) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("MapViewModel")
    }
}
