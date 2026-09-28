package io.github.halilozel1903.permissionflow

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.halilozel1903.permissionflow.core.PermissionStatus

/**
 * Shows [content] only while [state] is granted, and the right UI otherwise:
 *
 * - [permanentlyDenied] for [PermissionStatus.PermanentlyDenied] (default: [PermissionDeniedCard]
 *   with an "Open settings" button),
 * - [rationale] for [PermissionStatus.NotRequested] and [PermissionStatus.Denied] (default:
 *   [PermissionRequestCard] with an "Allow" button). Check `state.status.shouldShowRationale` in it
 *   to explain more after a denial.
 *
 * ```kotlin
 * val camera = rememberPermissionState(Manifest.permission.CAMERA)
 * PermissionGate(
 *     state = camera,
 *     rationale = { PermissionRequestCard(onAllow = it::launchRequest, title = "Camera needed") },
 *     permanentlyDenied = { PermissionDeniedCard(title = "Camera is turned off") },
 * ) {
 *     CameraPreview()
 * }
 * ```
 *
 * The gate never launches a request by itself; the status follows Settings changes on `ON_RESUME`.
 */
@Composable
public fun PermissionGate(
    state: PermissionRequestState,
    modifier: Modifier = Modifier,
    rationale: @Composable (PermissionRequestState) -> Unit = { PermissionRequestCard(onAllow = it::launchRequest) },
    permanentlyDenied: @Composable (PermissionRequestState) -> Unit = { PermissionDeniedCard() },
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        when (state.status) {
            PermissionStatus.Granted -> content()
            PermissionStatus.PermanentlyDenied -> permanentlyDenied(state)
            is PermissionStatus.Denied, PermissionStatus.NotRequested -> rationale(state)
        }
    }
}
