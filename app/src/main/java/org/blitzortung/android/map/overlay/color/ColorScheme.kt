/*

   Copyright 2015 Andreas Würl

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

package org.blitzortung.android.map.overlay.color

enum class ColorScheme(val strikeColors: IntArray) {
    BLITZORTUNG(intArrayOf(0xffe4f9f9.toInt(), 0xffd8f360.toInt(), 0xffdfbc51.toInt(), 0xffe48044.toInt(), 0xffe73c3b.toInt(), 0xffb82e2d.toInt())),
    RAINBOW(intArrayOf(0xffff0000.toInt(), 0xffff9900.toInt(), 0xffffff00.toInt(), 0xff99ff22.toInt(), 0xff00ffff.toInt(), 0xff6699ff.toInt())),
    TEMPERATURE(intArrayOf(0xffff5030.toInt(), 0xffe78060.toInt(), 0xffe0b0c0.toInt(), 0xffc0c0f0.toInt(), 0xff5080f0.toInt(), 0xff4060ff.toInt()));
}
