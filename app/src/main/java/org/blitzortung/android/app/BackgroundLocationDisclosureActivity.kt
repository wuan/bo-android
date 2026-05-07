package org.blitzortung.android.app

import android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.blitzortung.android.app.view.PreferenceKey
import org.blitzortung.android.app.view.put
import org.blitzortung.android.app.view.wasBackgroundLocationDisclosureShown
import org.blitzortung.android.util.isAtLeast

class BackgroundLocationDisclosureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)

        val alreadyDisclosed = preferences.wasBackgroundLocationDisclosureShown()
        val alreadyGranted = isAtLeast(Build.VERSION_CODES.Q) &&
            checkSelfPermission(ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (alreadyDisclosed || alreadyGranted) {
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setMessage(R.string.location_permission_background_disclosure)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                preferences.edit { put(PreferenceKey.BACKGROUND_LOCATION_DISCLOSURE_SHOWN, true) }
                if (isAtLeast(Build.VERSION_CODES.Q)) {
                    requestPermissions(arrayOf(ACCESS_BACKGROUND_LOCATION), REQUEST_CODE)
                } else {
                    finish()
                }
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Prevent dismissal via back press — user must make an explicit choice
    }

    companion object {
        private const val REQUEST_CODE = 200
    }
}
