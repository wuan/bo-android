package org.blitzortung.android.test

import android.content.Context
import android.content.SharedPreferences
import org.robolectric.RuntimeEnvironment

fun createPreferences(consumer: (SharedPreferences.Editor) -> Unit): SharedPreferences {
    val preferences = RuntimeEnvironment.application.getSharedPreferences("test-preferences", Context.MODE_PRIVATE);

    val edit: SharedPreferences.Editor = preferences.edit()
    edit.run(consumer)
    edit.commit();

    return preferences
}

