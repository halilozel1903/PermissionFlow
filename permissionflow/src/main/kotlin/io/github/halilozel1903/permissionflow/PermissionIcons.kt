package io.github.halilozel1903.permissionflow

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Small built-in icons, so the library doesn't depend on a Material icons artifact. */
public object PermissionIcons {

    /** A shield, used by the request card and the rationale sheet. */
    public val Shield: ImageVector by lazy {
        icon(
            "Shield",
            "M12,2L4,5v6c0,5.1 3.4,9.8 8,11c4.6,-1.2 8,-5.9 8,-11V5z" +
                "M10.6,15.6l-3.3,-3.3l1.4,-1.4l1.9,1.9l4.7,-4.7l1.4,1.4z",
        )
    }

    /** A crossed out circle, used by the permanently denied card. */
    public val Blocked: ImageVector by lazy {
        icon(
            "Blocked",
            "M12,2a10,10 0 1,0 0,20a10,10 0 1,0 0,-20z" +
                "M5.7,7.1l11.2,11.2a8,8 0 0,1 -11.2,-11.2z" +
                "M7.1,5.7a8,8 0 0,1 11.2,11.2z",
        )
    }

    /** Builds a 24 dp single path icon; holes are cut with the even-odd rule. */
    public fun icon(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(pathData = addPathNodes(pathData), pathFillType = PathFillType.EvenOdd, fill = SolidColor(Color.Black))
            .build()
}
