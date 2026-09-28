package io.github.halilozel1903.permissionflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A Material 3 bottom sheet that explains why the app needs a permission before asking for it, with
 * "Allow" and "Not now" buttons. Show it while the status is `Denied(shouldShowRationale = true)` (or
 * before the first request) and call `launchRequest()` from [onAllow].
 *
 * ```kotlin
 * var explain by remember { mutableStateOf(false) }
 * if (explain) {
 *     PermissionRationaleSheet(
 *         title = "Scan receipts with your camera",
 *         body = "The camera is only used while you scan. Photos stay on your phone.",
 *         onAllow = { explain = false; camera.launchRequest() },
 *         onDismiss = { explain = false },
 *     )
 * }
 * ```
 *
 * Both buttons hide the sheet with its animation first and then call their callback; swiping it
 * away or pressing back calls [onDismiss]. Remove the sheet from the composition in both callbacks.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun PermissionRationaleSheet(
    title: String,
    body: String,
    onAllow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = PermissionIcons.Shield,
    allowText: String = stringResource(R.string.permissionflow_allow),
    notNowText: String = stringResource(R.string.permissionflow_not_now),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val hideThen: (() -> Unit) -> Unit = { action ->
        scope.launch { sheetState.hide() }.invokeOnCompletion { cause -> if (cause == null) action() }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (icon != null) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = { hideThen(onDismiss) }) { Text(notNowText) }
                Button(onClick = { hideThen(onAllow) }) { Text(allowText) }
            }
        }
    }
}
