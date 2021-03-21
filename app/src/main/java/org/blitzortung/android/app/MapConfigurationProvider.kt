
import android.content.Context
import android.os.Environment
import android.util.Log
import org.osmdroid.api.IMapView
import org.osmdroid.config.DefaultConfigurationProvider
import org.osmdroid.tileprovider.util.StorageUtils
import java.io.File


class MapConfigurationProvider(private val context: Context) : DefaultConfigurationProvider() {
    override fun getOsmdroidBasePath(context: Context?): File? {
        try {
            if (osmdroidBasePath == null) {
                val storageInfo: StorageUtils.StorageInfo? = StorageUtils.getBestWritableStorage(context)
                if (storageInfo != null) {
                    val pathToStorage = storageInfo.path
                    osmdroidBasePath = File(pathToStorage, "osmdroid")
                    osmdroidBasePath.mkdirs()
                } else {
                    osmdroidBasePath = File((context ?: this.context).getExternalFilesDir(
                            Environment.DIRECTORY_PICTURES), "osmdroid")
                    if (!osmdroidBasePath.mkdirs()) {
                        Log.e(IMapView.LOGTAG, "Directory not created")
                    }
                }
            }
        } catch (ex: Exception) {
            Log.d(IMapView.LOGTAG, "Unable to create base path at $osmdroidBasePath", ex)
        }
        return osmdroidBasePath
    }
} 