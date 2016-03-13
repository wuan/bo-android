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
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import org.blitzortung.android.app.R
import org.blitzortung.android.dialogs.log.LogProvider

class LogDialog(
        context: Context,
        private val logProvider: LogProvider = LogProvider()
) : android.app.AlertDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.log_dialog)
    }

    override fun onStart() {
        super.onStart()

        setTitle(context.resources.getText(R.string.app_log))

        val logTextView = findViewById(R.id.log_text) as TextView
        logTextView.setHorizontallyScrolling(true)

        val logLines = logProvider.getLogLines()

        val logText = logLines.joinToString("\n")
        logTextView.text = logText

        val logButton = findViewById(R.id.log_send_email) as Button
        logButton.setOnClickListener { view -> composeEmail(logText) }
    }

    fun shareLog(logText: String) {
        Toast.makeText(context, "clicked", Toast.LENGTH_LONG).show()

        val intent = Intent(Intent.ACTION_SEND);
        intent.type = "text/plain"
        intent.data = Uri.parse("mailto:");
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.resources.getString(R.string.project_email)));
        intent.putExtra(Intent.EXTRA_SUBJECT, context.resources.getString(R.string.app_log))
        intent.putExtra(Intent.EXTRA_TEXT, logText)

        context.startActivity(Intent.createChooser(intent, context.resources.getString(R.string.share_log)));
    }

    fun composeEmail(logText: String) {
        val intent = Intent(Intent.ACTION_SENDTO);
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.resources.getString(R.string.project_email)));
        intent.putExtra(Intent.EXTRA_SUBJECT, context.resources.getString(R.string.app_log))
        intent.putExtra(Intent.EXTRA_TEXT, logText)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, context.resources.getString(R.string.share_unavailable), Toast.LENGTH_LONG).show()
            intent.data = null
            intent.action = Intent.ACTION_SEND
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent);
            }
        }
    }


    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            dismiss()
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
