package org.blitzortung.android.map

import android.content.Context
import org.osmdroid.config.DefaultConfigurationProvider
import java.io.File

class MapConfigurationProvider(private val context: Context) : DefaultConfigurationProvider() {
    override fun getOsmdroidBasePath(): File {
        return super.getOsmdroidBasePath(context)
    }
}