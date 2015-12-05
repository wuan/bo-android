package org.blitzortung.android.data.provider

import android.content.pm.PackageInfo

import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.beans.Station
import org.blitzortung.android.data.provider.result.ResultEvent

abstract class DataProvider {

    protected var username: String? = null

    protected var password: String? = null

    protected var pInfo: PackageInfo? = null

    abstract fun setUp()

    abstract fun shutDown()

    abstract fun getStrikes(parameters: Parameters, result: ResultEvent): ResultEvent

    abstract fun getStrikesGrid(parameters: Parameters, result: ResultEvent): ResultEvent

    abstract fun getStations(region: Int): List<Station>

    abstract val type: DataProviderType

    abstract fun reset()

    fun setCredentials(username: String, password: String) {
        this.username = username
        this.password = password
    }

    fun setPackageInfo(pInfo: PackageInfo) {
        this.pInfo = pInfo
    }

    abstract val isCapableOfHistoricalData: Boolean

}
