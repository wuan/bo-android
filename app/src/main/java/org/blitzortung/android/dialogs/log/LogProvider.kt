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
import java.util.*

class LogProvider {
    fun getLogLines(): List<String> {
        val process = Runtime.getRuntime().exec("logcat -d")
        val reader = BufferedReader(
                InputStreamReader(process.inputStream))

        val lines: ArrayList<String> = arrayListOf()
        reader.use {
            var line: String?
            do {
                line = reader.readLine()
                if (line == null)
                    break
                lines.add(line)
            } while (true)
        }
        return lines
    }
}