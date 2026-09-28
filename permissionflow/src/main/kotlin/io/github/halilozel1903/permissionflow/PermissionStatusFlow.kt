package io.github.halilozel1903.permissionflow

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.github.halilozel1903.permissionflow.core.PermissionController
import io.github.halilozel1903.permissionflow.core.PermissionStatus
import io.github.halilozel1903.permissionflow.core.PermissionsSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The status of [permission] as a cold [Flow] for code outside Compose. It emits the current status,
 * then again whenever [lifecycle] resumes (for example back from Settings) and after every request
 * made through PermissionFlow. Consecutive duplicates are dropped.
 *
 * With an activity context the rationale is read live. With the application context (for example in
 * a ViewModel) it falls back to the last value seen while an activity was around.
 *
 * ```kotlin
 * lifecycleScope.launch {
 *     repeatOnLifecycle(Lifecycle.State.STARTED) {
 *         permissionStatusFlow(Manifest.permission.CAMERA).collect { status -> render(status) }
 *     }
 * }
 * ```
 *
 * @param lifecycle refreshes on its `ON_RESUME`. Defaults to the activity's lifecycle; pass
 * `ProcessLifecycleOwner.get().lifecycle` from a ViewModel.
 */
public fun Context.permissionStatusFlow(
    permission: String,
    lifecycle: Lifecycle? = (findActivity() as? LifecycleOwner)?.lifecycle,
): Flow<PermissionStatus> =
    permissionsSnapshotFlow(listOf(permission), lifecycle)
        .map { it.statuses.getValue(permission) }
        .distinctUntilChanged()

/**
 * The statuses of [permissions] requested together, as a cold [Flow]. See [permissionStatusFlow].
 * [PermissionsSnapshot.status] combines them into one.
 */
public fun Context.multiplePermissionsStatusFlow(
    permissions: List<String>,
    lifecycle: Lifecycle? = (findActivity() as? LifecycleOwner)?.lifecycle,
): Flow<PermissionsSnapshot> = permissionsSnapshotFlow(permissions, lifecycle)

private fun Context.permissionsSnapshotFlow(permissions: List<String>, lifecycle: Lifecycle?): Flow<PermissionsSnapshot> {
    require(permissions.isNotEmpty()) { "At least one permission is required." }
    val context = this
    return callbackFlow {
        val controller = PermissionController(
            permissions,
            AndroidPermissionPlatform(context, context.findActivity()),
            PermissionFlow.recordStore(context),
        )
        send(controller.snapshot)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) trySend(controller.refresh())
        }
        lifecycle?.addObserver(observer)
        launch { PermissionFlow.changes.collect { send(controller.refresh()) } }
        awaitClose { lifecycle?.removeObserver(observer) }
    }
        // Lifecycle observers must be added and removed on the main thread.
        .flowOn(Dispatchers.Main.immediate)
        .distinctUntilChanged()
}

/**
 * Requests permissions from an activity without Compose. Create it with
 * [registerPermissionRequester] before the activity is started (as a property or in `onCreate`).
 */
public class PermissionRequester internal constructor(
    private val activity: ComponentActivity,
    permissions: List<String>,
    private val onResult: (PermissionsSnapshot) -> Unit,
) {
    private val permissions: List<String> = permissions.toList()

    init {
        require(permissions.isNotEmpty()) { "At least one permission is required." }
    }

    // Created lazily: the activity's context is not ready while its properties are initialized.
    private val controller by lazy(LazyThreadSafetyMode.NONE) {
        PermissionController(this.permissions, AndroidPermissionPlatform(activity, activity), PermissionFlow.recordStore(activity))
    }

    private val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val snapshot = controller.onRequestResult(results)
        PermissionFlow.notifyChanged()
        onResult(snapshot)
    }

    /** The current combined status, read now. Call it on the main thread once the activity is created. */
    public val status: PermissionStatus
        get() = controller.refresh().status

    /** The combined status as a [Flow] that refreshes on `ON_RESUME` and after requests. */
    public val statusFlow: Flow<PermissionStatus>
        get() = activity.multiplePermissionsStatusFlow(permissions, activity.lifecycle)
            .map { it.status }
            .distinctUntilChanged()

    /** Shows the system dialog for everything not granted yet, or reports the result right away when nothing is left. */
    public fun launch() {
        val toRequest = controller.prepareRequest()
        if (toRequest.isEmpty()) onResult(controller.snapshot) else launcher.launch(toRequest.toTypedArray())
    }
}

/**
 * Registers a [PermissionRequester] for [permissions]. Like every Activity Result API, call it
 * before the activity is started, for example as a property:
 *
 * ```kotlin
 * private val location = registerPermissionRequester(listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)) { result ->
 *     if (result.allGranted) startTracking()
 * }
 * ```
 */
public fun ComponentActivity.registerPermissionRequester(
    permissions: List<String>,
    onResult: (PermissionsSnapshot) -> Unit = {},
): PermissionRequester = PermissionRequester(this, permissions, onResult)

/** Registers a [PermissionRequester] for one [permission]. See the list overload. */
public fun ComponentActivity.registerPermissionRequester(
    permission: String,
    onResult: (PermissionStatus) -> Unit = {},
): PermissionRequester = PermissionRequester(this, listOf(permission)) { onResult(it.statuses.getValue(permission)) }
