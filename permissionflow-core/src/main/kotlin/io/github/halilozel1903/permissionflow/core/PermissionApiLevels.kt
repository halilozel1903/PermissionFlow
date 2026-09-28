package io.github.halilozel1903.permissionflow.core

import io.github.halilozel1903.permissionflow.core.AndroidPermission as P

/**
 * API level awareness: which runtime permissions to actually check and request for a permission on a
 * given Android version.
 *
 * - Permissions added in a later Android version are **granted implicitly** on older versions when
 *   the feature needed no runtime permission before (`POST_NOTIFICATIONS` below 33,
 *   `BLUETOOTH_CONNECT` below 31, `ACTIVITY_RECOGNITION` below 29, ...).
 * - Some are **replaced** by their predecessor (`READ_MEDIA_IMAGES` → `READ_EXTERNAL_STORAGE` below
 *   33, `NEARBY_WIFI_DEVICES` → `ACCESS_FINE_LOCATION` below 33, ...).
 * - `WRITE_EXTERNAL_STORAGE` grants nothing on API 30+ (scoped storage) and is granted implicitly there.
 *
 * Everything else is returned unchanged.
 */
public object PermissionApiLevels {

    /** API levels by name, to keep the rules readable. */
    private const val OREO = 26
    private const val PIE = 28
    private const val Q = 29
    private const val R = 30
    private const val S = 31
    private const val TIRAMISU = 33
    private const val UPSIDE_DOWN_CAKE = 34

    private class Rule(val introducedIn: Int, val replacement: List<String>)

    private val rules: Map<String, Rule> = mapOf(
        P.POST_NOTIFICATIONS to Rule(TIRAMISU, emptyList()),
        P.READ_MEDIA_IMAGES to Rule(TIRAMISU, listOf(P.READ_EXTERNAL_STORAGE)),
        P.READ_MEDIA_VIDEO to Rule(TIRAMISU, listOf(P.READ_EXTERNAL_STORAGE)),
        P.READ_MEDIA_AUDIO to Rule(TIRAMISU, listOf(P.READ_EXTERNAL_STORAGE)),
        P.READ_MEDIA_VISUAL_USER_SELECTED to Rule(UPSIDE_DOWN_CAKE, emptyList()),
        P.NEARBY_WIFI_DEVICES to Rule(TIRAMISU, listOf(P.ACCESS_FINE_LOCATION)),
        P.BODY_SENSORS_BACKGROUND to Rule(TIRAMISU, listOf(P.BODY_SENSORS)),
        P.BLUETOOTH_SCAN to Rule(S, listOf(P.ACCESS_FINE_LOCATION)),
        P.BLUETOOTH_CONNECT to Rule(S, emptyList()),
        P.BLUETOOTH_ADVERTISE to Rule(S, emptyList()),
        P.UWB_RANGING to Rule(S, emptyList()),
        P.ACCESS_BACKGROUND_LOCATION to Rule(Q, listOf(P.ACCESS_COARSE_LOCATION)),
        P.ACTIVITY_RECOGNITION to Rule(Q, emptyList()),
        P.ACCESS_MEDIA_LOCATION to Rule(Q, emptyList()),
        P.ACCEPT_HANDOVER to Rule(PIE, emptyList()),
        P.ANSWER_PHONE_CALLS to Rule(OREO, emptyList()),
        P.READ_PHONE_NUMBERS to Rule(OREO, listOf(P.READ_PHONE_STATE)),
    )

    /** The API level that introduced [permission] as a runtime permission, or `null` if it is not special cased. */
    public fun introducedIn(permission: String): Int? = rules[permission]?.introducedIn

    /**
     * The runtime permissions to check and request for [permission] on [sdkInt]. An empty list means
     * the permission is granted implicitly on that version.
     */
    public fun runtimePermissionsFor(permission: String, sdkInt: Int): List<String> {
        if (permission == P.WRITE_EXTERNAL_STORAGE && sdkInt >= R) return emptyList()
        val rule = rules[permission] ?: return listOf(permission)
        return if (sdkInt >= rule.introducedIn) listOf(permission) else rule.replacement
    }

    /** `true` when [permission] needs no runtime grant on [sdkInt]. */
    public fun isImplicitlyGranted(permission: String, sdkInt: Int): Boolean =
        runtimePermissionsFor(permission, sdkInt).isEmpty()

    /**
     * The permissions to request for reading shared media on [sdkInt]: the granular `READ_MEDIA_*`
     * permissions on API 33+, `READ_EXTERNAL_STORAGE` below.
     *
     * @param allowPartialAccess adds `READ_MEDIA_VISUAL_USER_SELECTED` on API 34+ for images or
     * video, so users can pick "Select photos and videos" instead of all or nothing.
     */
    public fun readMediaPermissions(
        sdkInt: Int,
        images: Boolean = true,
        video: Boolean = false,
        audio: Boolean = false,
        allowPartialAccess: Boolean = false,
    ): List<String> {
        require(images || video || audio) { "Ask for at least one of images, video or audio." }
        if (sdkInt < TIRAMISU) return listOf(P.READ_EXTERNAL_STORAGE)
        return buildList {
            if (images) add(P.READ_MEDIA_IMAGES)
            if (video) add(P.READ_MEDIA_VIDEO)
            if (audio) add(P.READ_MEDIA_AUDIO)
            if (allowPartialAccess && (images || video) && sdkInt >= UPSIDE_DOWN_CAKE) {
                add(P.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        }
    }
}
