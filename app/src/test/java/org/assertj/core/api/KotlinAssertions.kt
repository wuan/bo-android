@file:Suppress("unused")

package org.assertj.core.api

fun assertThat(str: String): AbstractCharSequenceAssert<*, String> = StringAssert(str)

fun assertThat(actual: Int): AbstractIntegerAssert<*> = IntegerAssert(actual)
fun assertThat(actual: Long): AbstractLongAssert<*> = LongAssert(actual)
fun assertThat(actual: Float): AbstractFloatAssert<*> = FloatAssert(actual)
fun assertThat(actual: Double): AbstractDoubleAssert<*> = DoubleAssert(actual)

fun assertThat(actual: Boolean): AbstractBooleanAssert<*> = BooleanAssert(actual)

fun <T> assertThat(actual: T): AbstractObjectAssert<*, T> = ObjectAssert(actual)