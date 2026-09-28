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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * A card that asks for a permission: icon, title, body and an "Allow" button. The default
 * `rationale` of [PermissionGate].
 */
@Composable
public fun PermissionRequestCard(
    onAllow: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.permissionflow_request_title),
    body: String = stringResource(R.string.permissionflow_request_body),
    icon: ImageVector? = PermissionIcons.Shield,
    allowText: String = stringResource(R.string.permissionflow_allow),
) {
    PermissionMessageCard(
        icon = icon,
        title = title,
        body = body,
        buttonText = allowText,
        onClick = onAllow,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        iconContainerColor = MaterialTheme.colorScheme.secondary,
        iconColor = MaterialTheme.colorScheme.onSecondary,
        modifier = modifier,
    )
}

/**
 * A card for a permanently denied permission, with an "Open settings" button that opens the app's
 * system settings page. PermissionFlow refreshes the status when the user comes back.
 *
 * @param onOpenSettings defaults to [openAppSettings].
 */
@Composable
public fun PermissionDeniedCard(
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.permissionflow_denied_title),
    body: String = stringResource(R.string.permissionflow_denied_body),
    icon: ImageVector? = PermissionIcons.Blocked,
    openSettingsText: String = stringResource(R.string.permissionflow_open_settings),
    onOpenSettings: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    PermissionMessageCard(
        icon = icon,
        title = title,
        body = body,
        buttonText = openSettingsText,
        onClick = { if (onOpenSettings != null) onOpenSettings() else context.openAppSettings() },
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        iconContainerColor = MaterialTheme.colorScheme.error,
        iconColor = MaterialTheme.colorScheme.onError,
        modifier = modifier,
    )
}

@Composable
private fun PermissionMessageCard(
    icon: ImageVector?,
    title: String,
    body: String,
    buttonText: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    iconContainerColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (icon != null) {
                    Surface(shape = CircleShape, color = iconContainerColor, contentColor = iconColor) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick, modifier = Modifier.align(Alignment.End)) { Text(buttonText) }
        }
    }
}
