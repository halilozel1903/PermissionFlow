package io.github.halilozel1903.permissionflow.core

import io.github.halilozel1903.permissionflow.core.AndroidPermission as P
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PermissionControllerTest {
    private val platform = FakePlatform()
    private val store = InMemoryPermissionRecordStore()

    private fun controller(vararg permissions: String, platform: PermissionPlatform = this.platform) =
        PermissionController(permissions.toList(), platform, store)

    private fun PermissionController.ask(answer: Boolean?): PermissionStatus {
        val toRequest = prepareRequest()
        return onRequestResult(platform.request(toRequest, answer)).status
    }

    @Test
    fun `grant on first request`() {
        val camera = controller(P.CAMERA)
        assertEquals(PermissionStatus.NotRequested, camera.snapshot.status)
        assertEquals(PermissionStatus.Granted, camera.ask(true))
    }

    @Test
    fun `deny twice becomes permanently denied`() {
        val camera = controller(P.CAMERA)
        assertEquals(PermissionStatus.Denied(shouldShowRationale = true), camera.ask(false))
        assertEquals(PermissionStatus.PermanentlyDenied, camera.ask(false))
        // Asking again returns at once without a dialog and stays permanently denied.
        assertEquals(PermissionStatus.PermanentlyDenied, camera.ask(true))
    }

    @Test
    fun `permanent denial survives a restart`() {
        controller(P.CAMERA).apply { ask(false); ask(false) }
        assertEquals(PermissionStatus.PermanentlyDenied, controller(P.CAMERA).snapshot.status)
    }

    @Test
    fun `dismissed dialog is denied without rationale, not permanent`() {
        val camera = controller(P.CAMERA)
        assertEquals(PermissionStatus.Denied(shouldShowRationale = false), camera.ask(null))
        assertEquals(PermissionStatus.Denied(shouldShowRationale = true), camera.ask(false))
        assertEquals(PermissionStatus.Granted, camera.ask(true))
    }

    @Test
    fun `blocked by policy is permanent after the second silent denial`() {
        platform.blocked += P.CAMERA
        val camera = controller(P.CAMERA)
        assertEquals(PermissionStatus.Denied(shouldShowRationale = false), camera.ask(true))
        assertEquals(PermissionStatus.PermanentlyDenied, camera.ask(true))
    }

    @Test
    fun `granting in settings is picked up on refresh`() {
        val camera = controller(P.CAMERA).apply { ask(false); ask(false) }
        platform.grantInSettings(P.CAMERA)
        assertEquals(PermissionStatus.Granted, camera.refresh().status)
    }

    @Test
    fun `revoked in settings starts over`() {
        val camera = controller(P.CAMERA)
        camera.ask(false)
        camera.ask(true)
        platform.revokeInSettings(P.CAMERA)
        assertEquals(PermissionStatus.NotRequested, camera.refresh().status)
        assertEquals(PermissionStatus.Denied(true), camera.ask(false))
    }

    @Test
    fun `cancelled request is not counted`() {
        val camera = controller(P.CAMERA)
        camera.prepareRequest()
        assertEquals(PermissionStatus.NotRequested, camera.onRequestResult(emptyMap()).status)
        assertEquals(PermissionRecord.Empty, store.read(P.CAMERA))
    }

    @Test
    fun `notifications are granted below 33 and never requested`() {
        val old = FakePlatform(sdkInt = 32)
        val notifications = controller(P.POST_NOTIFICATIONS, platform = old)
        assertEquals(PermissionStatus.Granted, notifications.snapshot.status)
        assertTrue(notifications.prepareRequest().isEmpty())
    }

    @Test
    fun `media permissions share one runtime permission below 33`() {
        val old = FakePlatform(sdkInt = 30)
        val media = controller(P.READ_MEDIA_IMAGES, P.READ_MEDIA_VIDEO, platform = old)
        assertEquals(listOf(P.READ_EXTERNAL_STORAGE), media.prepareRequest())
        val snapshot = media.onRequestResult(old.request(listOf(P.READ_EXTERNAL_STORAGE), answer = true))
        assertTrue(snapshot.allGranted)
    }

    @Test
    fun `multiple permissions combine and request only what is missing`() {
        platform.granted += P.ACCESS_COARSE_LOCATION
        val location = controller(P.ACCESS_FINE_LOCATION, P.ACCESS_COARSE_LOCATION, P.ACCESS_FINE_LOCATION)
        assertEquals(listOf(P.ACCESS_FINE_LOCATION, P.ACCESS_COARSE_LOCATION), location.permissions)
        assertEquals(PermissionStatus.NotRequested, location.snapshot.status)
        assertEquals(listOf(P.ACCESS_FINE_LOCATION), location.prepareRequest())
        assertEquals(listOf(P.ACCESS_FINE_LOCATION), location.snapshot.revoked)
    }

    @Test
    fun `subset requests only its own permissions`() {
        val both = controller(P.CAMERA, P.RECORD_AUDIO)
        assertEquals(listOf(P.RECORD_AUDIO), both.prepareRequest(listOf(P.RECORD_AUDIO)))
        assertFailsWith<IllegalArgumentException> { both.prepareRequest(listOf(P.READ_CONTACTS)) }
    }

    @Test
    fun `without an activity the last known rationale is used`() {
        controller(P.CAMERA).ask(false)
        val noActivity = FakePlatform().apply { hasActivity = false }
        assertEquals(PermissionStatus.Denied(true), controller(P.CAMERA, platform = noActivity).snapshot.status)
    }

    @Test
    fun `empty permission list is rejected`() {
        assertFailsWith<IllegalArgumentException> { PermissionController(emptyList(), platform, store) }
    }
}
