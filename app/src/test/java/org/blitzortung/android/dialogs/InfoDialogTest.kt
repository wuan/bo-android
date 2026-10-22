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

package org.blitzortung.android.dialogs

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.blitzortung.android.app.components.BuildVersion
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class InfoDialogTest {
    @Test
    fun buildsDialogWithVersionInformation() {
        val context = RuntimeEnvironment.getApplication()
        val buildVersion = mockk<BuildVersion>(relaxed = true)
        every { buildVersion.versionName } returns "1.2.3"
        every { buildVersion.versionCode } returns 123

        val dialog = InfoDialog(context, buildVersion)

        assertThat(dialog.isShowing).isFalse
    }
}
