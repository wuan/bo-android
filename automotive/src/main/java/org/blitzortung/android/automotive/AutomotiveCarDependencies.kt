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

package org.blitzortung.android.automotive

import javax.inject.Inject
import javax.inject.Singleton
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.car.CarDependencies
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository
import org.blitzortung.android.map.overlay.color.StrikeColorHandler

/**
 * Android Automotive OS implementation of the car experience's dependencies,
 * backed by the shared `:core` graph so the embedded app reuses the same data,
 * location and alert logic as the phone app.
 */
@Singleton
class AutomotiveCarDependencies
    @Inject
    constructor(
        override val strikeDataRepository: StrikeDataRepository,
        override val locationRepository: LocationRepository,
        override val alertRepository: AlertRepository,
        override val strikeColorHandler: StrikeColorHandler,
    ) : CarDependencies
