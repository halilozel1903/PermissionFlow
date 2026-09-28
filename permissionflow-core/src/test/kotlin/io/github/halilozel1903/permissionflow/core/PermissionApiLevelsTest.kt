package io.github.halilozel1903.permissionflow.core

import io.github.halilozel1903.permissionflow.core.AndroidPermission as P
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermissionApiLevelsTest {
    @Test
    fun `notifications need a runtime grant only on 33 and above`() {
        assertTrue(PermissionApiLevels.isImplicitlyGranted(P.POST_NOTIFICATIONS, 32))
        assertEquals(listOf(P.POST_NOTIFICATIONS), PermissionApiLevels.runtimePermissionsFor(P.POST_NOTIFICATIONS, 33))
        assertEquals(33, PermissionApiLevels.introducedIn(P.POST_NOTIFICATIONS))
    }

    @Test
    fun `media permissions map to external storage below 33`() {
        assertEquals(listOf(P.READ_EXTERNAL_STORAGE), PermissionApiLevels.runtimePermissionsFor(P.READ_MEDIA_IMAGES, 32))
        assertEquals(listOf(P.READ_EXTERNAL_STORAGE), PermissionApiLevels.runtimePermissionsFor(P.READ_MEDIA_VIDEO, 24))
        assertEquals(listOf(P.READ_MEDIA_AUDIO), PermissionApiLevels.runtimePermissionsFor(P.READ_MEDIA_AUDIO, 33))
        assertTrue(PermissionApiLevels.isImplicitlyGranted(P.READ_MEDIA_VISUAL_USER_SELECTED, 33))
    }

    @Test
    fun `older replacements`() {
        assertEquals(listOf(P.ACCESS_FINE_LOCATION), PermissionApiLevels.runtimePermissionsFor(P.BLUETOOTH_SCAN, 30))
        assertEquals(listOf(P.BLUETOOTH_SCAN), PermissionApiLevels.runtimePermissionsFor(P.BLUETOOTH_SCAN, 31))
        assertTrue(PermissionApiLevels.isImplicitlyGranted(P.BLUETOOTH_CONNECT, 30))
        assertEquals(listOf(P.ACCESS_COARSE_LOCATION), PermissionApiLevels.runtimePermissionsFor(P.ACCESS_BACKGROUND_LOCATION, 28))
        assertEquals(listOf(P.ACCESS_FINE_LOCATION), PermissionApiLevels.runtimePermissionsFor(P.NEARBY_WIFI_DEVICES, 32))
        assertEquals(listOf(P.READ_PHONE_STATE), PermissionApiLevels.runtimePermissionsFor(P.READ_PHONE_NUMBERS, 25))
        assertTrue(PermissionApiLevels.isImplicitlyGranted(P.ACTIVITY_RECOGNITION, 28))
    }

    @Test
    fun `write external storage is a no-op on scoped storage`() {
        assertEquals(listOf(P.WRITE_EXTERNAL_STORAGE), PermissionApiLevels.runtimePermissionsFor(P.WRITE_EXTERNAL_STORAGE, 29))
        assertTrue(PermissionApiLevels.isImplicitlyGranted(P.WRITE_EXTERNAL_STORAGE, 30))
    }

    @Test
    fun `other permissions are unchanged`() {
        assertEquals(listOf(P.CAMERA), PermissionApiLevels.runtimePermissionsFor(P.CAMERA, 24))
        assertFalse(PermissionApiLevels.isImplicitlyGranted("com.example.CUSTOM", 36))
        assertNull(PermissionApiLevels.introducedIn(P.CAMERA))
    }

    @Test
    fun `read media helper`() {
        assertEquals(listOf(P.READ_EXTERNAL_STORAGE), PermissionApiLevels.readMediaPermissions(32, images = true, video = true))
        assertEquals(listOf(P.READ_MEDIA_IMAGES, P.READ_MEDIA_VIDEO), PermissionApiLevels.readMediaPermissions(33, video = true, allowPartialAccess = true))
        assertEquals(
            listOf(P.READ_MEDIA_IMAGES, P.READ_MEDIA_VISUAL_USER_SELECTED),
            PermissionApiLevels.readMediaPermissions(34, allowPartialAccess = true),
        )
        assertEquals(listOf(P.READ_MEDIA_AUDIO), PermissionApiLevels.readMediaPermissions(34, images = false, audio = true, allowPartialAccess = true))
        assertFailsWith<IllegalArgumentException> { PermissionApiLevels.readMediaPermissions(34, images = false) }
    }
}
