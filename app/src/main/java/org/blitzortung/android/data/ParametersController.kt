package org.blitzortung.android.data

class ParametersController private constructor(private val offsetIncrement: Int) {

    fun rewInterval(parameters: Parameters): Parameters {
        return updateInterval(parameters, -offsetIncrement)
    }

    fun ffwdInterval(parameters: Parameters): Parameters {
        return updateInterval(parameters, offsetIncrement)
    }

    fun updateInterval(parameters: Parameters, offsetIncrement: Int): Parameters {
        var intervalOffset = parameters.intervalOffset + offsetIncrement
        val intervalDuration = parameters.intervalDuration

        if (intervalOffset < -MAX_RANGE + intervalDuration) {
            intervalOffset = -MAX_RANGE + intervalDuration
        } else if (intervalOffset > 0) {
            intervalOffset = 0
        }

        return parameters.copy(intervalOffset = alignValue(intervalOffset))
    }

    fun goRealtime(parameters: Parameters): Parameters {
        return parameters.copy(intervalOffset = 0)
    }

    private fun alignValue(value: Int): Int {
        return (value / offsetIncrement) * offsetIncrement
    }

    companion object {

        private val MAX_RANGE = 24 * 60

        fun withOffsetIncrement(offsetIncrement: Int): ParametersController {
            return ParametersController(offsetIncrement)
        }
    }

}
