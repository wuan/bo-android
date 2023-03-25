package org.blitzortung.android.preferences

class SliderData(
    private val suffix: String,
    val default: Int,
    private val maximum: Int,
    private val minimum: Int,
    private val step: Int,
) {
    var value: Int = applyConstraints(default)
        set(value) {
            field = applyConstraints(value)
        }
        get() = field

    var offset: Int
        set(offset) {
            value = offset + minimum
        }
        get() = value - minimum

    val size get() = maximum - minimum
    val text get() = "$value $suffix"

    private fun applyConstraints(value: Int) = (value.coerceIn(minimum, maximum).plus(step / 2) / step).toInt() * step
}