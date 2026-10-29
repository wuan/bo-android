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

package org.blitzortung.android.car

import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository

/**
 * The data-layer dependencies the car experience needs. Implemented by each
 * application module (phone / Android Automotive OS) and registered in
 * [CarDependenciesProvider], so that the framework-instantiated car service and
 * screens can reach the same repositories as the phone UI without depending on an
 * application module.
 */
interface CarDependencies {
    val strikeDataRepository: StrikeDataRepository

    val locationRepository: LocationRepository

    val alertRepository: AlertRepository
}
