package org.blitzortung.android.util

fun isAtLeast(versionCode: Int) : Boolean {
    return android.os.Build.VERSION.SDK_INT >= versionCode
}

