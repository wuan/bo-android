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

/**
 * Which palette treatment to apply to strikes.
 *
 * The names are historical (they date from when the phone offered satellite and
 * street-map tiles). Both the phone and the car always draw MAPNIK tiles, so the
 * values really select a shading direction:
 *
 * - [SATELLITE] is the dark-shading target: the raw bright palette for a dark map.
 * - [STREETMAP] is the bright-shading target: the same palette darkened to 80%
 *   for a pale map.
 */
enum class ColorTarget {
    /** Dark-shading palette: the raw bright strike colours. */
    SATELLITE,

    /** Bright-shading palette: strike colours darkened for a pale map. */
    STREETMAP,
}
