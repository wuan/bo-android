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

package org.blitzortung.android.data.provider

/**
 * Signals that strike data could not be retrieved from a [data.DataProvider], wrapping the
 * underlying I/O or parsing failure as an unchecked exception.
 */
class DataProviderException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)
