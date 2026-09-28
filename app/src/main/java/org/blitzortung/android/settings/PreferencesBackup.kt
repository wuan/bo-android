package org.blitzortung.android.settings

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Serialization, validation and reconciliation for user-initiated preference backups.
 *
 * A backup is a versioned JSON document:
 * ```
 * {
 *   "format": "bo-android/preferences",
 *   "version": 1,
 *   "appVersionCode": 123,
 *   "preferences": { "alarm_enabled": true, "map_scale": 75 }
 * }
 * ```
 *
 * Credentials and device-specific values are never written to or restored from a
 * backup, regardless of whether the source file contains them.
 */
object PreferencesBackup {
    const val FORMAT_ID = "bo-android/preferences"
    const val FORMAT_VERSION = 1

    internal const val FIELD_FORMAT = "format"
    internal const val FIELD_VERSION = "version"
    internal const val FIELD_APP_VERSION_CODE = "appVersionCode"
    internal const val FIELD_PREFERENCES = "preferences"

    /** Keys that must never leave the device in a shareable backup. */
    val EXCLUDED_KEYS: Set<String> = setOf("username", "password", "osmdroid.basePath")

    /**
     * Renders the given preference values as a backup document. Values keep their
     * native JSON type; unsupported value types are skipped.
     */
    fun serialize(
        preferences: Map<String, Any?>,
        appVersionCode: Int,
    ): String {
        val preferencesJson = JSONObject()
        preferences.forEach { (key, value) ->
            if (key in EXCLUDED_KEYS) {
                return@forEach
            }
            when (value) {
                is Boolean, is Int, is Long, is Float, is Double, is String -> preferencesJson.put(key, value)
                else -> Unit
            }
        }

        return JSONObject()
            .put(FIELD_FORMAT, FORMAT_ID)
            .put(FIELD_VERSION, FORMAT_VERSION)
            .put(FIELD_APP_VERSION_CODE, appVersionCode)
            .put(FIELD_PREFERENCES, preferencesJson)
            .toString(2)
    }

    /**
     * Parses and structurally validates a backup document without touching any
     * preference. On success the raw preference values are returned.
     */
    fun parse(json: String): BackupParseResult {
        val root =
            try {
                JSONObject(json)
            } catch (e: JSONException) {
                return BackupParseResult.Failure(BackupFailureReason.MALFORMED_JSON, e.message)
            }

        val format = root.optString(FIELD_FORMAT, null)
        if (format != FORMAT_ID) {
            return BackupParseResult.Failure(
                BackupFailureReason.NOT_A_BACKUP,
                "expected format '$FORMAT_ID' but found '${format ?: "<missing>"}'",
            )
        }

        val version = root.optInt(FIELD_VERSION, -1)
        if (version != FORMAT_VERSION) {
            return BackupParseResult.Failure(
                BackupFailureReason.UNSUPPORTED_VERSION,
                "expected version $FORMAT_VERSION but found ${root.opt(FIELD_VERSION)?.toString() ?: "<missing>"}",
            )
        }

        val preferencesJson =
            root.optJSONObject(FIELD_PREFERENCES)
                ?: return BackupParseResult.Failure(
                    BackupFailureReason.INVALID_PREFERENCES,
                    "missing or invalid '$FIELD_PREFERENCES' object",
                )

        val preferences = LinkedHashMap<String, Any>()
        val keys = preferencesJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = preferencesJson.opt(key)
            if (value == null || value == JSONObject.NULL || value is JSONArray) {
                continue
            }
            preferences[key] = value
        }

        return BackupParseResult.Success(
            appVersionCode = root.optInt(FIELD_APP_VERSION_CODE, -1),
            preferences = preferences,
        )
    }

    /**
     * Coerces the recognized entries of a parsed backup to the exact types currently
     * held by the device and classifies the differences against the device's keys.
     *
     * Unknown file-only keys are ignored; recognized keys absent from the file are
     * reported as missing (they will fall back to their defaults on import).
     * A file entry whose type cannot be coerced to the device's type fails the whole plan.
     */
    fun planImport(
        filePreferences: Map<String, Any>,
        devicePreferences: Map<String, Any>,
    ): ImportPlanResult {
        val toApply = LinkedHashMap<String, Any>()
        val unknownKeys = mutableListOf<String>()

        filePreferences.forEach { (key, value) ->
            if (key in EXCLUDED_KEYS) {
                return@forEach
            }
            val expected = devicePreferences[key]
            if (expected == null) {
                unknownKeys += key
                return@forEach
            }
            val coerced =
                coerce(value, expected)
                    ?: return ImportPlanResult.Failure(key, value)
            toApply[key] = coerced
        }

        val missingKeys =
            devicePreferences.keys
                .filter { it !in filePreferences && it !in EXCLUDED_KEYS }
                .sorted()

        return ImportPlanResult.Success(
            ImportPlan(
                toApply = toApply,
                unknownKeys = unknownKeys.sorted(),
                missingKeys = missingKeys,
            ),
        )
    }

    internal fun coerce(
        value: Any,
        expected: Any,
    ): Any? =
        when (expected) {
            is Boolean -> value as? Boolean
            is Int ->
                when (value) {
                    is Int -> value
                    is Long -> value.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
                    else -> null
                }

            is Long ->
                when (value) {
                    is Int -> value.toLong()
                    is Long -> value
                    else -> null
                }

            is Float ->
                when (value) {
                    is Int -> value.toFloat()
                    is Long -> value.toFloat()
                    is Float -> value
                    is Double -> value.toFloat()
                    else -> null
                }

            is Double ->
                when (value) {
                    is Int -> value.toDouble()
                    is Long -> value.toDouble()
                    is Float -> value.toDouble()
                    is Double -> value
                    else -> null
                }

            is String -> value as? String
            else -> null
        }
}

sealed interface BackupParseResult {
    data class Success(
        val appVersionCode: Int,
        val preferences: Map<String, Any>,
    ) : BackupParseResult

    data class Failure(
        val reason: BackupFailureReason,
        val detail: String? = null,
    ) : BackupParseResult
}

enum class BackupFailureReason {
    MALFORMED_JSON,
    NOT_A_BACKUP,
    UNSUPPORTED_VERSION,
    INVALID_PREFERENCES,
}

sealed interface ImportPlanResult {
    data class Success(val plan: ImportPlan) : ImportPlanResult

    data class Failure(
        val key: String,
        val value: Any,
    ) : ImportPlanResult
}

/**
 * Reconciliation of a backup file against the device's current preferences.
 *
 * @property toApply recognized keys already coerced to their expected device types
 * @property unknownKeys file keys the application does not recognize (ignored)
 * @property missingKeys recognized device keys absent from the file (reset to defaults)
 */
data class ImportPlan(
    val toApply: Map<String, Any>,
    val unknownKeys: List<String>,
    val missingKeys: List<String>,
)