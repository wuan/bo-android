/*

   Copyright 2025 Andreas Würl

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

package org.blitzortung.android.data

import org.blitzortung.android.util.isAtLeast
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

class SequenceValidator @Inject constructor() {

    private val currentSequenceNumber = AtomicLong()

    fun isUpdate(sequenceNumber: Long) =
        sequenceNumber == determineUpdatedSequenceNumber(sequenceNumber)

    private fun determineUpdatedSequenceNumber(sequenceNumber: Long) = if (isAtLeast(24)) {
        currentSequenceNumber.updateAndGet { previousSequenceNumber ->
            if (previousSequenceNumber < sequenceNumber) sequenceNumber else previousSequenceNumber
        }
    } else {
        synchronized(currentSequenceNumber) {
            val previousSequenceNumber = currentSequenceNumber.get()
            val updated = if (previousSequenceNumber < sequenceNumber) sequenceNumber else previousSequenceNumber
            currentSequenceNumber.set(updated)
            updated
        }
    }

}
