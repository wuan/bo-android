package org.assertj.core.api

@Suppress("unused")
class KotlinAssertions {
    companion object {
        fun assertThat(str: String) : StringAssert {
            return StringAssert(str)
        }

        fun assertThat(actual: Int): AbstractIntegerAssert<*> {
            return IntegerAssert(actual)
        }

        fun assertThat(actual: Float): AbstractFloatAssert<*> {
            return FloatAssert(actual)
        }

        fun assertThat(actual: Double): AbstractDoubleAssert<*> {
            return DoubleAssert(actual)
        }

        fun assertThat(actual: Boolean): AbstractBooleanAssert<*> {
            return BooleanAssert(actual)
        }

        fun <T> assertThat(actual: T): AbstractObjectAssert<*, T> {
            return ObjectAssert(actual)
        }
    }
}