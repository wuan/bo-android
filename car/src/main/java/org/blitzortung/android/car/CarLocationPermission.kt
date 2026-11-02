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

import android.Manifest
import android.content.pm.PackageManager
import androidx.car.app.CarContext
import androidx.car.app.OnRequestPermissionsListener

/**
 * Requests location access through the car host. The car app cannot show the
 * phone's permission dialog, so it must ask the host; when access is denied the
 * configured default/manual location remains the fallback.
 */
class CarLocationPermission(
    private val carContext: CarContext,
) {
    fun isGranted(): Boolean =
        LOCATION_PERMISSIONS.all {
            carContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }

    fun request(onResult: (Boolean) -> Unit) {
        if (isGranted()) {
            onResult(true)
            return
        }

        carContext.requestPermissions(
            LOCATION_PERMISSIONS,
            OnRequestPermissionsListener { granted, _ ->
                onResult(granted.isNotEmpty())
            },
        )
    }

    companion object {
        val LOCATION_PERMISSIONS = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    }
}
