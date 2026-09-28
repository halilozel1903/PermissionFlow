package io.github.halilozel1903.permissionflow.sample

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.permissionflow.FakeMultiplePermissionsState
import io.github.halilozel1903.permissionflow.FakePermissionState
import io.github.halilozel1903.permissionflow.PermissionDeniedCard
import io.github.halilozel1903.permissionflow.PermissionGate
import io.github.halilozel1903.permissionflow.PermissionIcons
import io.github.halilozel1903.permissionflow.PermissionRationaleSheet
import io.github.halilozel1903.permissionflow.PermissionRequestCard
import io.github.halilozel1903.permissionflow.PermissionRequestState
import io.github.halilozel1903.permissionflow.core.AndroidPermission
import io.github.halilozel1903.permissionflow.core.PermissionStatus
import io.github.halilozel1903.permissionflow.openAppSettings
import io.github.halilozel1903.permissionflow.rememberMultiplePermissionsState
import io.github.halilozel1903.permissionflow.rememberPermissionState

/**
 * Real permission requests by default. `scripts/screenshots.sh` starts the app with
 * `--es scene <scene>` to show faked statuses without any system dialog:
 *
 * - `overview`: camera allowed, location denied (rationale), notifications blocked
 * - `rationale`: the same list with the location rationale sheet open
 * - `denied`: camera permanently denied, the scanner shows the "Open settings" card
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val scene = intent.getStringExtra(EXTRA_SCENE)?.takeIf { it in SCENES }
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                if (scene == null) LiveApp() else FakeApp(scene)
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
        private val SCENES = setOf("overview", "rationale", "denied")
    }
}

/** What the sample asks for, and why. */
private enum class Feature(
    val title: String,
    val why: String,
    val icon: ImageVector,
    val rationaleTitle: String,
    val rationaleBody: String,
) {
    Camera(
        title = "Camera",
        why = "Scan receipts and QR codes",
        icon = SampleIcons.Camera,
        rationaleTitle = "Scan receipts with your camera",
        rationaleBody = "The camera is only used while the scanner is open. Photos never leave your phone.",
    ),
    Location(
        title = "Location",
        why = "Sort stores by distance",
        icon = SampleIcons.Location,
        rationaleTitle = "Show stores near you",
        rationaleBody = "Your location is used only while the app is open, to sort stores by distance. " +
            "You can change this at any time in Settings.",
    ),
    Notifications(
        title = "Notifications",
        why = "Hear when your order is ready",
        icon = SampleIcons.Bell,
        rationaleTitle = "Know when your order is ready",
        rationaleBody = "We send one notification when your order is ready for pickup. No marketing, promise.",
    ),
}

@Composable
private fun LiveApp() {
    val camera = rememberPermissionState(Manifest.permission.CAMERA)
    val location = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
    )
    // A runtime permission on API 33+ only; PermissionFlow reports it as granted below.
    val notifications = rememberPermissionState(AndroidPermission.POST_NOTIFICATIONS)
    PermissionsScreen(mapOf(Feature.Camera to camera, Feature.Location to location, Feature.Notifications to notifications))
}

/** Faked statuses for screenshots: "Allow" just flips the fake to granted, no system dialog is ever shown. */
@Composable
private fun FakeApp(scene: String) {
    val states = remember(scene) {
        val grant: (FakePermissionState) -> Unit = { it.status = PermissionStatus.Granted }
        val denied = scene == "denied"
        val camera = FakePermissionState(
            Manifest.permission.CAMERA,
            if (denied) PermissionStatus.PermanentlyDenied else PermissionStatus.Granted,
            grant,
        )
        val locationStatus = if (denied) PermissionStatus.Granted else PermissionStatus.Denied(shouldShowRationale = true)
        val location = FakeMultiplePermissionsState(
            listOf(
                FakePermissionState(Manifest.permission.ACCESS_FINE_LOCATION, locationStatus, grant),
                FakePermissionState(Manifest.permission.ACCESS_COARSE_LOCATION, locationStatus, grant),
            ),
        ) { state -> state.permissions.forEach { it.status = PermissionStatus.Granted } }
        val notifications = FakePermissionState(
            AndroidPermission.POST_NOTIFICATIONS,
            if (denied) PermissionStatus.NotRequested else PermissionStatus.PermanentlyDenied,
            grant,
        )
        mapOf(Feature.Camera to camera, Feature.Location to location, Feature.Notifications to notifications)
    }
    PermissionsScreen(states, initialRationale = if (scene == "rationale") Feature.Location else null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsScreen(
    states: Map<Feature, PermissionRequestState>,
    initialRationale: Feature? = null,
) {
    val context = LocalContext.current
    var rationaleFor by rememberSaveable { mutableStateOf(initialRationale) }
    val camera = states.getValue(Feature.Camera)

    fun onAction(feature: Feature) {
        val state = states.getValue(feature)
        when (state.status) {
            PermissionStatus.Granted -> Unit
            PermissionStatus.NotRequested -> state.launchRequest()
            is PermissionStatus.Denied -> rationaleFor = feature
            PermissionStatus.PermanentlyDenied -> context.openAppSettings()
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("PermissionFlow") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Receipt scanner")
            PermissionGate(
                state = camera,
                rationale = {
                    PermissionRequestCard(
                        onAllow = { onAction(Feature.Camera) },
                        title = "Camera needed to scan",
                        body = "Allow the camera to scan receipts and QR codes.",
                        icon = SampleIcons.Camera,
                    )
                },
                permanentlyDenied = {
                    PermissionDeniedCard(
                        title = "Camera access is off",
                        body = "You turned off camera access for this app. Allow it in Settings to scan receipts.",
                        icon = SampleIcons.Camera,
                    )
                },
            ) {
                Viewfinder()
            }

            SectionTitle("Permissions", Modifier.padding(top = 8.dp))
            Feature.entries.forEach { feature ->
                PermissionRow(feature, states.getValue(feature).status, onAction = { onAction(feature) })
            }
        }
    }

    rationaleFor?.let { feature ->
        PermissionRationaleSheet(
            title = feature.rationaleTitle,
            body = feature.rationaleBody,
            icon = feature.icon,
            onAllow = {
                rationaleFor = null
                states.getValue(feature).launchRequest()
            },
            onDismiss = { rationaleFor = null },
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun PermissionRow(feature: Feature, status: PermissionStatus, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                    Icon(feature.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    feature.why,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusChip(status)
            }
            when (status) {
                PermissionStatus.Granted -> Unit
                PermissionStatus.PermanentlyDenied -> OutlinedButton(onClick = onAction) { Text("Settings") }
                else -> FilledTonalButton(onClick = onAction) { Text("Allow") }
            }
        }
    }
}

@Composable
private fun StatusChip(status: PermissionStatus) {
    val colors = MaterialTheme.colorScheme
    val (label, container, content) = when (status) {
        PermissionStatus.Granted -> Triple("Allowed", colors.primary, colors.onPrimary)
        is PermissionStatus.Denied -> Triple(
            if (status.shouldShowRationale) "Denied · explain why" else "Denied",
            colors.tertiaryContainer,
            colors.onTertiaryContainer,
        )
        PermissionStatus.PermanentlyDenied -> Triple("Blocked · Settings only", colors.error, colors.onError)
        PermissionStatus.NotRequested -> Triple("Not asked yet", colors.surfaceVariant, colors.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = container, contentColor = content) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A stand-in for a camera preview. */
@Composable
private fun Viewfinder() {
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFF1B1F24), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 220.dp, height = 140.dp)) {
            val len = 28.dp.toPx()
            val stroke = 4.dp.toPx()
            val w = size.width
            val h = size.height
            listOf(
                Offset(0f, 0f) to Offset(1f, 1f),
                Offset(w, 0f) to Offset(-1f, 1f),
                Offset(0f, h) to Offset(1f, -1f),
                Offset(w, h) to Offset(-1f, -1f),
            ).forEach { (corner, dir) ->
                drawLine(accent, corner, corner + Offset(dir.x * len, 0f), stroke, StrokeCap.Round)
                drawLine(accent, corner, corner + Offset(0f, dir.y * len), stroke, StrokeCap.Round)
            }
            drawLine(
                Color(0xFFEF5350),
                Offset(len / 2, h / 2),
                Offset(w - len / 2, h / 2),
                2.dp.toPx(),
            )
        }
        Text(
            "Point the camera at a receipt",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp),
        )
    }
}

/** Sample icons drawn with PermissionFlow's tiny path helper. */
private object SampleIcons {
    val Camera: ImageVector = PermissionIcons.icon(
        "Camera",
        "M4,7h3.2l1.8,-2h6l1.8,2H20a1,1 0 0,1 1,1v11a1,1 0 0,1 -1,1H4a1,1 0 0,1 -1,-1V8a1,1 0 0,1 1,-1z" +
            "M12,9a4,4 0 1,0 0,8a4,4 0 1,0 0,-8z",
    )
    val Location: ImageVector = PermissionIcons.icon(
        "Location",
        "M12,2a7,7 0 0,0 -7,7c0,5.2 7,13 7,13s7,-7.8 7,-13a7,7 0 0,0 -7,-7z" +
            "M12,6.5a2.5,2.5 0 1,0 0,5a2.5,2.5 0 1,0 0,-5z",
    )
    val Bell: ImageVector = PermissionIcons.icon(
        "Bell",
        "M12,22a2,2 0 0,0 2,-2h-4a2,2 0 0,0 2,2z" +
            "M18,16v-5c0,-3.1 -1.6,-5.6 -4.5,-6.3V4a1.5,1.5 0 0,0 -3,0v0.7C7.6,5.4 6,7.9 6,11v5l-2,2v1h16v-1z",
    )
}
