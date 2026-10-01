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

package org.blitzortung.android.data.beans

import java.io.Serializable

/**
 * A stored strike cluster returned by the RPC backend
 * (`get_global_clusters` / `get_local_clusters`).
 *
 * [shape] is the cluster's outer boundary as a closed ring of `(longitude, latitude)`
 * pairs (a GeoJSON `LineString`); it is empty when the backend could not build a
 * shape. [area] is the geodesic polygon area in km², or `null` when no shape exists.
 */
data class Cluster(
    val id: Long,
    val timestamp: Long,
    val intervalSeconds: Int,
    val strikeCount: Int,
    val area: Double?,
    val shape: List<Pair<Double, Double>>,
) : Serializable
