package org.blitzortung.android.map.overlay

interface LayerOverlay {
    val name: String
    var enabled: Boolean
    var visible: Boolean
}
