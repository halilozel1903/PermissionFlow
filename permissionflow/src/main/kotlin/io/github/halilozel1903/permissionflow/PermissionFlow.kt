package io.github.halilozel1903.permissionflow

import android.content.Context
import android.os.Build
import io.github.halilozel1903.permissionflow.core.PermissionApiLevels
import io.github.halilozel1903.permissionflow.core.PermissionRecordStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** Entry point for helpers that don't need Compose. */
public object PermissionFlow {

    @Volatile
    private var store: PermissionRecordStore? = null

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emits after any permission request made through PermissionFlow returned, so other observers refresh. */
    internal val changes: SharedFlow<Unit> get() = _changes

    internal fun notifyChanged() {
        _changes.tryEmit(Unit)
    }

    /** The process wide record store (request history per permission). */
    internal fun recordStore(context: Context): PermissionRecordStore =
        store ?: synchronized(this) {
            store ?: SharedPreferencesPermissionRecordStore(context).also { store = it }
        }

    /**
     * The runtime permissions [permission] maps to on this device, for example `READ_EXTERNAL_STORAGE`
     * for `READ_MEDIA_IMAGES` below API 33, or an empty list for `POST_NOTIFICATIONS` below 33.
     * PermissionFlow applies this mapping by itself; use it to know what to declare in the manifest.
     */
    public fun runtimePermissionsFor(permission: String): List<String> =
        PermissionApiLevels.runtimePermissionsFor(permission, Build.VERSION.SDK_INT)

    /** `true` when [permission] needs no runtime grant on this device (for example `POST_NOTIFICATIONS` below 33). */
    public fun isImplicitlyGranted(permission: String): Boolean =
        PermissionApiLevels.isImplicitlyGranted(permission, Build.VERSION.SDK_INT)

    /**
     * The permissions to request for reading photos, videos or audio on this device: `READ_MEDIA_*`
     * on API 33+, `READ_EXTERNAL_STORAGE` below.
     *
     * ```kotlin
     * val media = rememberMultiplePermissionsState(PermissionFlow.readMediaPermissions(images = true, video = true))
     * ```
     */
    public fun readMediaPermissions(
        images: Boolean = true,
        video: Boolean = false,
        audio: Boolean = false,
        allowPartialAccess: Boolean = false,
    ): List<String> = PermissionApiLevels.readMediaPermissions(Build.VERSION.SDK_INT, images, video, audio, allowPartialAccess)
}
