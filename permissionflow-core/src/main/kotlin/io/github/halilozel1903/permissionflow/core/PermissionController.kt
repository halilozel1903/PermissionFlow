package io.github.halilozel1903.permissionflow.core

/** What the platform reports about runtime permissions. The Android library implements it with `ContextCompat` and `ActivityCompat`. */
public interface PermissionPlatform {
    /** `Build.VERSION.SDK_INT`. */
    public val sdkInt: Int

    /** `checkSelfPermission(permission) == PERMISSION_GRANTED`. */
    public fun isGranted(permission: String): Boolean

    /** `shouldShowRequestPermissionRationale(permission)`, or `null` when there is no activity to ask. */
    public fun shouldShowRationale(permission: String): Boolean?
}

/**
 * The statuses of a set of permissions at one moment.
 *
 * @property statuses the status of each permission, in the order they were given.
 */
public data class PermissionsSnapshot(val statuses: Map<String, PermissionStatus>) {
    /** All statuses [combined] into one. */
    val status: PermissionStatus get() = statuses.values.combined()

    /** `true` when every permission is granted. */
    val allGranted: Boolean get() = statuses.values.all { it.isGranted }

    /** The permissions that are not granted. */
    val revoked: List<String> get() = statuses.filterValues { !it.isGranted }.keys.toList()
}

/**
 * Tracks and requests a set of permissions. It owns the bookkeeping that Android leaves to apps:
 * API level mapping, request history and permanent denial. The UI layer only has to launch the
 * system request with [prepareRequest] and report the result with [onRequestResult].
 *
 * Not thread safe; use it from one thread (the main thread on Android).
 *
 * @param permissions the permissions as the app names them; they are mapped for the platform's API
 * level with [PermissionApiLevels].
 */
public class PermissionController(
    permissions: List<String>,
    private val platform: PermissionPlatform,
    private val store: PermissionRecordStore,
) {
    init {
        require(permissions.isNotEmpty()) { "At least one permission is required." }
    }

    /** The permissions this controller tracks, without duplicates. */
    public val permissions: List<String> = permissions.distinct()

    private val runtimePermissions: Map<String, List<String>> =
        this.permissions.associateWith { PermissionApiLevels.runtimePermissionsFor(it, platform.sdkInt) }

    /** The latest snapshot, updated by [refresh], [prepareRequest] and [onRequestResult]. */
    public var snapshot: PermissionsSnapshot = read()
        private set

    /** Reads the current state from the platform, for example when the app comes back from Settings. */
    public fun refresh(): PermissionsSnapshot {
        snapshot = read()
        return snapshot
    }

    /**
     * Refreshes and returns the runtime permissions to hand to the system request for [subset]
     * (default: all), without duplicates and without the ones already granted. An empty list means
     * there is nothing to ask for: report the current [snapshot] right away.
     */
    public fun prepareRequest(subset: Collection<String> = permissions): List<String> {
        require(permissions.containsAll(subset)) { "Unknown permissions: ${subset - permissions.toSet()}" }
        refresh()
        return subset
            .flatMap { runtimePermissions.getValue(it) }
            .distinct()
            .filterNot { platform.isGranted(it) }
    }

    /**
     * Records the result of a system permission request (`RequestMultiplePermissions`) and refreshes.
     * An empty result means the request was cancelled (for example the activity was destroyed) and
     * is not counted.
     */
    public fun onRequestResult(results: Map<String, Boolean>): PermissionsSnapshot {
        results.keys.forEach { permission -> store.write(permission, store.read(permission).afterRequest()) }
        return refresh()
    }

    private fun read(): PermissionsSnapshot {
        val runtimeStatuses = runtimePermissions.values.flatten().distinct().associateWith(::readRuntime)
        return PermissionsSnapshot(
            permissions.associateWith { permission ->
                runtimePermissions.getValue(permission).map(runtimeStatuses::getValue).combined()
            },
        )
    }

    private fun readRuntime(permission: String): PermissionStatus {
        val granted = platform.isGranted(permission)
        val rationale = if (granted) null else platform.shouldShowRationale(permission)
        val old = store.read(permission)
        val record = old.observe(granted, rationale)
        if (record != old) store.write(permission, record)
        return PermissionStatusResolver.resolve(granted, rationale, record)
    }
}
