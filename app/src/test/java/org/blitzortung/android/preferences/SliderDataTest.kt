package org.blitzortung.android.preferences

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SliderDataTest {
    private lateinit var uut: SliderData

    @Before
    fun setUp() {
        uut = SliderData("foo", 25, 60, 20, 10)
    }

    @Test
    fun checkStartValues() {
        assertThat(uut.default).isEqualTo(25)
        assertThat(uut.size).isEqualTo(40)
        assertThat(uut.value).isEqualTo(30)
        assertThat(uut.offset).isEqualTo(10)
        assertThat(uut.text).isEqualTo("30 foo")
    }

    @Test
    fun minValue() {
        uut.value = 20
        assertThat(uut.value).isEqualTo(20)
        assertThat(uut.offset).isEqualTo(0)
    }

    @Test
    fun minOffset() {
        uut.offset = 0
        assertThat(uut.value).isEqualTo(20)
        assertThat(uut.offset).isEqualTo(0)
    }

    @Test
    fun coerceMinValue() {
        uut.value = 10
        assertThat(uut.value).isEqualTo(20)
    }

    @Test
    fun maxValue() {
        uut.value = 60
        assertThat(uut.value).isEqualTo(60)
        assertThat(uut.offset).isEqualTo(40)
    }

    @Test
    fun coerceMaxValue() {
        uut.value = 70
        assertThat(uut.value).isEqualTo(60)
    }
}