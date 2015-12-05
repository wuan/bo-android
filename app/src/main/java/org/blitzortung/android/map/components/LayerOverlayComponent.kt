package org.blitzortung.android.map.components

import org.blitzortung.android.map.overlay.LayerOverlay

class LayerOverlayComponent(
        override val name: String,
        override var visible: Boolean = true,
        override var enabled: kotlin.Boolean = true
) : LayerOverlay {

}
