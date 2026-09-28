package io.github.halilozel1903.permissionflow

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Opens this app's page in the system settings, where the user can allow a permanently denied
 * permission. PermissionFlow refreshes every state when the user comes back (`ON_RESUME`).
 *
 * @return `false` if no settings screen could be opened.
 */
public fun Context.openAppSettings(): Boolean = startSettings(
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
)

/**
 * Opens this app's notification settings (API 26+), or its app settings page on older versions.
 * Useful below API 33, where notifications need no permission but can still be turned off.
 *
 * @return `false` if no settings screen could be opened.
 */
public fun Context.openNotificationSettings(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        if (startSettings(intent)) return true
    }
    return openAppSettings()
}

private fun Context.startSettings(intent: Intent): Boolean {
    if (findActivity() == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
