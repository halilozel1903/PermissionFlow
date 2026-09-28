package io.github.halilozel1903.permissionflow.core

/**
 * The status of a runtime permission (or of a group of permissions requested together).
 *
 * ```kotlin
 * when (status) {
 *     PermissionStatus.Granted -> ShowCamera()
 *     is PermissionStatus.Denied -> if (status.shouldShowRationale) ExplainWhy() else AskAgain()
 *     PermissionStatus.PermanentlyDenied -> OpenSettings()
 *     PermissionStatus.NotRequested -> Ask()
 * }
 * ```
 */
public sealed interface PermissionStatus {

    /** The permission is granted, or not needed on this API level (for example `POST_NOTIFICATIONS` below 33). */
    public data object Granted : PermissionStatus

    /**
     * The user denied the permission, but the app can still ask again.
     *
     * @property shouldShowRationale `true` when the system recommends explaining why the app needs
     * the permission before asking again (`shouldShowRequestPermissionRationale`).
     */
    public data class Denied(val shouldShowRationale: Boolean) : PermissionStatus

    /**
     * The system no longer shows the permission dialog ("Don't ask again", denied twice on Android 11+
     * or blocked by a device policy). Only the app's system settings page can grant it now.
     */
    public data object PermanentlyDenied : PermissionStatus

    /** The app has not asked for the permission yet. */
    public data object NotRequested : PermissionStatus
}

/** `true` for [PermissionStatus.Granted]. */
public val PermissionStatus.isGranted: Boolean
    get() = this == PermissionStatus.Granted

/** `true` when the app should explain why it needs the permission before asking again. */
public val PermissionStatus.shouldShowRationale: Boolean
    get() = this is PermissionStatus.Denied && shouldShowRationale

/** `true` for [PermissionStatus.PermanentlyDenied]. */
public val PermissionStatus.isPermanentlyDenied: Boolean
    get() = this == PermissionStatus.PermanentlyDenied

/**
 * Combines the statuses of permissions that are requested together into one status:
 *
 * - [PermissionStatus.Granted] when every one is granted (or the collection is empty),
 * - [PermissionStatus.PermanentlyDenied] when any missing one is permanently denied, because the whole
 *   set can then only be completed in the system settings,
 * - [PermissionStatus.Denied] when any is denied, with a rationale if any of them wants one,
 * - [PermissionStatus.NotRequested] otherwise.
 */
public fun Iterable<PermissionStatus>.combined(): PermissionStatus {
    val missing = filterNot { it.isGranted }
    return when {
        missing.isEmpty() -> PermissionStatus.Granted
        missing.any { it.isPermanentlyDenied } -> PermissionStatus.PermanentlyDenied
        missing.any { it is PermissionStatus.Denied } ->
            PermissionStatus.Denied(shouldShowRationale = missing.any { it.shouldShowRationale })
        else -> PermissionStatus.NotRequested
    }
}
