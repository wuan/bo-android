package org.blitzortung.android.alert.data

data class AlertSectorRange(
        val rangeMinimum: Float,
        val rangeMaximum: Float,
        val strikeCount: Int,
        val latestStrikeTimestamp: Long
) {}
