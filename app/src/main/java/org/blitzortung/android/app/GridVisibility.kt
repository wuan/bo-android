/*

   Copyright 2026 Andreas Würl

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

package org.blitzortung.android.app

/**
 * Decides whether the grid/strike overlay should be drawn.
 *
 * The grid is shown by default and can only be hidden while clusters are shown on a
 * provider that supports them. This guarantees that the map never ends up with both
 * overlays disabled (an empty map).
 */
internal fun shouldShowGrid(
    hideGrid: Boolean,
    showClusters: Boolean,
    clustersSupported: Boolean,
): Boolean = !hideGrid || !showClusters || !clustersSupported
