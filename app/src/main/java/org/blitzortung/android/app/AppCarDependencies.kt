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

import javax.inject.Inject
import javax.inject.Singleton
import org.blitzortung.android.alert.AlertRepository
import org.blitzortung.android.car.CarDependencies
import org.blitzortung.android.data.repository.StrikeDataRepository
import org.blitzortung.android.location.LocationRepository

/**
 * Phone application's implementation of the car experience's dependencies,
 * backed by the shared Dagger graph so the car app reuses the same repositories
 * (and therefore the same cache and update scheduling) as the phone UI.
 */
@Singleton
class AppCarDependencies
    @Inject
    constructor(
        override val strikeDataRepository: StrikeDataRepository,
        override val locationRepository: LocationRepository,
        override val alertRepository: AlertRepository,
    ) : CarDependencies
