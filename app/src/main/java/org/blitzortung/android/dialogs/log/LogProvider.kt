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

package org.blitzortung.android.dialogs.log

import java.io.BufferedReader
import java.io.InputStreamReader

class LogProvider {
    fun getLogLines(): List<String> {
        val process = Runtime.getRuntime().exec("logcat -d -t $MAX_LOG_LINES")
        return try {
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.lineSequence().toList()
            }
        } finally {
            process.destroy()
        }
    }

    companion object {
        const val MAX_LOG_LINES = 2000
    }
}
