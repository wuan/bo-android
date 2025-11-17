/*

   Copyright 2016 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.dialogs

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import org.blitzortung.android.app.R
import org.blitzortung.android.app.components.BuildVersion
import org.blitzortung.android.data.cache.CacheSize
import org.blitzortung.android.dialogs.log.LogProvider

class LogDialog(
    context: Context,
    private val cacheSize: CacheSize,
    private val buildVersion: BuildVersion,
    private val logProvider: LogProvider = LogProvider(),
) : android.app.AlertDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.log_dialog)
    }

    override fun onStart() {
        super.onStart()

        val versionText = getVersionString()
        val deviceText = getDeviceString()
        val cacheText = getCacheString()
        val logLines = logProvider.getLogLines()

        val logText = versionText + "\n\n" + deviceText + "\n\n" + cacheText + "\n\n" + logLines.joinToString("\n")

        with(findViewById<TextView>(R.id.log_text)) {
            setHorizontallyScrolling(true)
            text = logText
        }

        with(findViewById<Button>(R.id.log_send_email)) {
            setOnClickListener { composeEmail(logText) }
        }

        with(findViewById<Button>(R.id.log_cancel)) {
            setOnClickListener { dismiss() }
        }
    }

    private fun getCacheString(): String {
        return "Cache size: %d entries, %d strikes".format(cacheSize.entries, cacheSize.strikes)
    }

    private fun getDeviceString(): String {
        val device = Build.DEVICE // Device
        val model = Build.MODEL // Model
        val product = Build.PRODUCT // Product
        val version = System.getProperty("os.version") // OS version
        val sdkInt = Build.VERSION.SDK_INT // API Level

        return "Device: $device, model: $model, Product: $product, Version: $version, SDK: $sdkInt"
    }

    private fun getVersionString(): String {
        return buildVersion.run { "Version $versionName ($versionCode)" }
    }

    private fun composeEmail(body: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = "mailto:".toUri()
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.resources.getString(R.string.project_email)))
        intent.putExtra(Intent.EXTRA_SUBJECT, context.resources.getString(R.string.app_log_subject))
        intent.putExtra(
            Intent.EXTRA_TEXT,
            if (body.length > MAX_LOG_SIZE) {
                body.substring(body.length - MAX_LOG_SIZE)
            } else {
                body
            },
        )
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, context.resources.getString(R.string.share_unavailable), Toast.LENGTH_LONG).show()
            intent.data = null
            intent.action = Intent.ACTION_SEND
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        const val MAX_LOG_SIZE = 250000
    }
}
