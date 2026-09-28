package io.github.halilozel1903.permissionflow

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.halilozel1903.permissionflow.core.PermissionController
import io.github.halilozel1903.permissionflow.core.PermissionStatus
import io.github.halilozel1903.permissionflow.core.PermissionsSnapshot
import io.github.halilozel1903.permissionflow.core.combined
import io.github.halilozel1903.permissionflow.core.isGranted

/** Something that has a [PermissionStatus] and can ask the user for it: one permission or several. */
@Stable
public interface PermissionRequestState {
    /** The current status. Reading it in a composable recomposes when it changes. */
    public val status: PermissionStatus

    /**
     * Shows the system permission dialog for everything that is not granted yet. Call it from a
     * click handler or an effect, never during composition. When nothing is left to ask for, the
     * `onResult` callback runs right away.
     */
    public fun launchRequest()
}

/** The state of one runtime permission. Create it with [rememberPermissionState]. */
@Stable
public interface PermissionState : PermissionRequestState {
    /** The permission, for example `Manifest.permission.CAMERA`. */
    public val permission: String
}

/** The state of permissions that are requested together. Create it with [rememberMultiplePermissionsState]. */
@Stable
public interface MultiplePermissionsState : PermissionRequestState {
    /** Each permission's own state. [PermissionState.launchRequest] on one of them asks for that one only. */
    public val permissions: List<PermissionState>

    /** All statuses [combined]: `Granted` only when every permission is granted. */
    override val status: PermissionStatus
        get() = permissions.map { it.status }.combined()

    /** `true` when every permission is granted. */
    public val allPermissionsGranted: Boolean
        get() = permissions.all { it.status.isGranted }

    /** The permissions that are not granted. */
    public val revokedPermissions: List<PermissionState>
        get() = permissions.filterNot { it.status.isGranted }
}

/**
 * Remembers the state of [permission]. The status updates after a request and whenever the screen
 * resumes, for example when the user comes back from the app's system settings.
 *
 * ```kotlin
 * val camera = rememberPermissionState(Manifest.permission.CAMERA)
 * when (camera.status) {
 *     PermissionStatus.Granted -> CameraPreview()
 *     PermissionStatus.PermanentlyDenied -> PermissionDeniedCard()
 *     else -> Button(onClick = camera::launchRequest) { Text("Allow camera") }
 * }
 * ```
 *
 * Permissions that don't exist on the device's Android version are mapped for you: for example
 * `POST_NOTIFICATIONS` is [PermissionStatus.Granted] below API 33 and `READ_MEDIA_IMAGES` checks
 * `READ_EXTERNAL_STORAGE` there. In `@Preview` the state is a [FakePermissionState].
 *
 * @param onResult called with the new status after a request returned.
 */
@Composable
public fun rememberPermissionState(
    permission: String,
    onResult: (PermissionStatus) -> Unit = {},
): PermissionState {
    if (LocalInspectionMode.current) return remember(permission) { FakePermissionState(permission) }
    val latestOnResult = rememberUpdatedState(onResult)
    val holder = rememberPermissionsHolder(listOf(permission)) { snapshot ->
        latestOnResult.value(snapshot.statuses.getValue(permission))
    }
    return remember(holder) { PermissionStateImpl(permission, holder) }
}

/**
 * Remembers the state of [permissions] requested together, for example fine and coarse location.
 * See [rememberPermissionState].
 *
 * @param onResult called with every permission's status after a request returned.
 */
@Composable
public fun rememberMultiplePermissionsState(
    permissions: List<String>,
    onResult: (Map<String, PermissionStatus>) -> Unit = {},
): MultiplePermissionsState {
    require(permissions.isNotEmpty()) { "At least one permission is required." }
    if (LocalInspectionMode.current) {
        return remember(permissions) { FakeMultiplePermissionsState(permissions.distinct().map(::FakePermissionState)) }
    }
    val latestOnResult = rememberUpdatedState(onResult)
    val holder = rememberPermissionsHolder(permissions) { snapshot -> latestOnResult.value(snapshot.statuses) }
    return remember(holder) { MultiplePermissionsStateImpl(holder) }
}

@Composable
private fun rememberPermissionsHolder(
    permissions: List<String>,
    onResult: (PermissionsSnapshot) -> Unit,
): PermissionsHolder {
    val context = LocalContext.current
    val activity = LocalActivity.current ?: context.findActivity()
    val holder = remember(permissions, context, activity) {
        val platform = AndroidPermissionPlatform(context, activity)
        PermissionsHolder(PermissionController(permissions, platform, PermissionFlow.recordStore(context)), onResult)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        holder.onRequestResult(results)
    }
    DisposableEffect(holder, launcher) {
        holder.launcher = launcher
        onDispose { holder.launcher = null }
    }
    // Replays ON_RESUME when the screen is already resumed, and fires again after Settings or the dialog.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { holder.refresh() }
    LaunchedEffect(holder) { PermissionFlow.changes.collect { holder.refresh() } }
    return holder
}

/** Bridges the pure [PermissionController] and Compose: every change goes through a snapshot state. */
@Stable
internal class PermissionsHolder(
    private val controller: PermissionController,
    private val onResult: (PermissionsSnapshot) -> Unit,
) {
    val permissions: List<String> get() = controller.permissions

    var snapshot: PermissionsSnapshot by mutableStateOf(controller.snapshot)
        private set

    var launcher: ActivityResultLauncher<Array<String>>? = null

    fun refresh() {
        snapshot = controller.refresh()
    }

    fun launch(subset: List<String>) {
        val toRequest = controller.prepareRequest(subset)
        snapshot = controller.snapshot
        if (toRequest.isEmpty()) {
            onResult(snapshot)
            return
        }
        val launcher = checkNotNull(launcher) {
            "launchRequest() was called before the permission state was attached. " +
                "Call it from a click handler or an effect, not during composition."
        }
        launcher.launch(toRequest.toTypedArray())
    }

    fun onRequestResult(results: Map<String, Boolean>) {
        snapshot = controller.onRequestResult(results)
        PermissionFlow.notifyChanged()
        onResult(snapshot)
    }
}

private class PermissionStateImpl(
    override val permission: String,
    private val holder: PermissionsHolder,
) : PermissionState {
    override val status: PermissionStatus
        get() = holder.snapshot.statuses.getValue(permission)

    override fun launchRequest() = holder.launch(listOf(permission))

    override fun toString(): String = "PermissionState($permission, $status)"
}

private class MultiplePermissionsStateImpl(private val holder: PermissionsHolder) : MultiplePermissionsState {
    override val permissions: List<PermissionState> = holder.permissions.map { PermissionStateImpl(it, holder) }

    override val status: PermissionStatus
        get() = holder.snapshot.status

    override fun launchRequest() = holder.launch(holder.permissions)

    override fun toString(): String = "MultiplePermissionsState(${holder.snapshot.statuses})"
}

/**
 * A [PermissionState] you control, for tests, previews and screenshots. Change [status] to simulate
 * the user's answer.
 *
 * ```kotlin
 * val camera = FakePermissionState(Manifest.permission.CAMERA, PermissionStatus.PermanentlyDenied)
 * PermissionGate(camera) { CameraPreview() }   // shows the "Open settings" card
 * ```
 */
@Stable
public class FakePermissionState(
    override val permission: String,
    status: PermissionStatus = PermissionStatus.NotRequested,
    private val onLaunchRequest: (FakePermissionState) -> Unit = {},
) : PermissionState {
    override var status: PermissionStatus by mutableStateOf(status)

    override fun launchRequest(): Unit = onLaunchRequest(this)

    override fun toString(): String = "FakePermissionState($permission, $status)"
}

/** A [MultiplePermissionsState] you control, for tests, previews and screenshots. */
@Stable
public class FakeMultiplePermissionsState(
    override val permissions: List<FakePermissionState>,
    private val onLaunchRequest: (FakeMultiplePermissionsState) -> Unit = {},
) : MultiplePermissionsState {
    override fun launchRequest(): Unit = onLaunchRequest(this)

    override fun toString(): String = "FakeMultiplePermissionsState($permissions)"
}
