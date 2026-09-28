package io.github.halilozel1903.permissionflow

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.halilozel1903.permissionflow.core.PermissionPlatform
import io.github.halilozel1903.permissionflow.core.PermissionRecord
import io.github.halilozel1903.permissionflow.core.PermissionRecordStore

/** Reads permission state from Android. The rationale needs an activity; without one it is unknown. */
internal class AndroidPermissionPlatform(
    private val context: Context,
    private val activity: Activity?,
) : PermissionPlatform {
    override val sdkInt: Int get() = Build.VERSION.SDK_INT

    override fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    override fun shouldShowRationale(permission: String): Boolean? =
        activity?.let { ActivityCompat.shouldShowRequestPermissionRationale(it, permission) }
}

/** Keeps [PermissionRecord]s in a private SharedPreferences file, so they survive process death and restarts. */
internal class SharedPreferencesPermissionRecordStore(context: Context) : PermissionRecordStore {
    private val prefs = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    override fun read(permission: String): PermissionRecord = PermissionRecord(
        requestCount = prefs.getInt("$permission$REQUESTS", 0).coerceAtLeast(0),
        rationaleSeen = prefs.getBoolean("$permission$RATIONALE_SEEN", false),
        lastRationale = prefs.getBoolean("$permission$LAST_RATIONALE", false),
    )

    override fun write(permission: String, record: PermissionRecord) {
        val editor = prefs.edit()
        if (record == PermissionRecord.Empty) {
            editor.remove("$permission$REQUESTS").remove("$permission$RATIONALE_SEEN").remove("$permission$LAST_RATIONALE")
        } else {
            editor.putInt("$permission$REQUESTS", record.requestCount)
                .putBoolean("$permission$RATIONALE_SEEN", record.rationaleSeen)
                .putBoolean("$permission$LAST_RATIONALE", record.lastRationale)
        }
        editor.apply()
    }

    private companion object {
        const val FILE_NAME = "io.github.halilozel1903.permissionflow"
        const val REQUESTS = ":requests"
        const val RATIONALE_SEEN = ":rationaleSeen"
        const val LAST_RATIONALE = ":lastRationale"
    }
}

/** The activity behind this context, if any. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
