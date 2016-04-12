package org.blitzortung.android.app

import android.app.Fragment
import android.os.Bundle
import android.util.Log
import org.blitzortung.android.alert.handler.AlertHandler
import org.blitzortung.android.data.Parameters
import org.blitzortung.android.data.provider.result.DataEvent
import org.blitzortung.android.location.LocationHandler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject

class StateFragment(
        val locationHandler: LocationHandler,
        val alertHandler: AlertHandler
) : Fragment() {

    val parametersObservable = PublishSubject.create<Parameters>()
    val dataObservable = BehaviorSubject.create<DataEvent>()

    init {
        dataObservable.subscribeOn(AndroidSchedulers.mainThread()).observeOn(Schedulers.io())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(Main.LOG_TAG, "StateFragment.onCreate()")
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    val data: DataEvent
        get() = dataObservable?.value ?: DataEvent()

    companion object {
        val TAG = "BOStateFragment"
    }
}

