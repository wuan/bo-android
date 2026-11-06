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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapShadeOverlayTest {
    @Test
    fun `dark shading uses a black scrim`() {
        val overlay = MapShadeOverlay()

        overlay.applyShading(MapShading.DARK)

        assertThat(overlay.color).isEqualTo(Color.BLACK)
        assertThat(overlay.alpha).isEqualTo(MapShadeOverlay.DARK_ALPHA)
        assertThat(overlay.alpha).isEqualTo(128)
    }

    @Test
    fun `bright shading uses a white scrim`() {
        val overlay = MapShadeOverlay()

        overlay.applyShading(MapShading.BRIGHT)

        assertThat(overlay.color).isEqualTo(Color.WHITE)
        assertThat(overlay.alpha).isEqualTo(MapShadeOverlay.BRIGHT_ALPHA)
        assertThat(overlay.alpha).isEqualTo(140)
    }

    @Test
    fun `alpha zero draws nothing`() {
        val overlay = MapShadeOverlay()
        overlay.alpha = 0
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)

        overlay.draw(Canvas(bitmap), null, false)

        assertThat(bitmap.getPixel(WIDTH / 2, HEIGHT / 2)).isEqualTo(Color.TRANSPARENT)
    }

    private companion object {
        const val WIDTH = 8
        const val HEIGHT = 8
    }
}
