package io.github.halilozel1903.permissionflow.core

import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionStatusResolverTest {
    private fun resolve(granted: Boolean, rationale: Boolean?, record: PermissionRecord) =
        PermissionStatusResolver.resolve(granted, rationale, record.observe(granted, rationale))

    @Test
    fun `granted wins over everything`() {
        assertEquals(PermissionStatus.Granted, resolve(true, false, PermissionRecord(requestCount = 5, rationaleSeen = true)))
    }

    @Test
    fun `never requested and no rationale is not requested`() {
        assertEquals(PermissionStatus.NotRequested, resolve(false, false, PermissionRecord.Empty))
    }

    @Test
    fun `rationale means denied with rationale`() {
        assertEquals(PermissionStatus.Denied(true), resolve(false, true, PermissionRecord(requestCount = 1)))
    }

    @Test
    fun `rationale gone after it was seen means permanently denied`() {
        val record = PermissionRecord(requestCount = 2, rationaleSeen = true, lastRationale = true)
        assertEquals(PermissionStatus.PermanentlyDenied, resolve(false, false, record))
    }

    @Test
    fun `one request without rationale is a dismissed dialog`() {
        assertEquals(PermissionStatus.Denied(false), resolve(false, false, PermissionRecord(requestCount = 1)))
    }

    @Test
    fun `two requests without any rationale are permanently denied`() {
        assertEquals(PermissionStatus.PermanentlyDenied, resolve(false, false, PermissionRecord(requestCount = 2)))
    }

    @Test
    fun `unknown rationale falls back to the last known one`() {
        val record = PermissionRecord(requestCount = 1, rationaleSeen = true, lastRationale = true)
        assertEquals(PermissionStatus.Denied(true), resolve(false, null, record))
        assertEquals(PermissionStatus.PermanentlyDenied, resolve(false, null, record.copy(lastRationale = false)))
        assertEquals(PermissionStatus.NotRequested, resolve(false, null, PermissionRecord.Empty))
    }

    @Test
    fun `granted clears the record`() {
        val record = PermissionRecord(requestCount = 3, rationaleSeen = true, lastRationale = true)
        assertEquals(PermissionRecord.Empty, record.observe(isGranted = true, shouldShowRationale = null))
    }

    @Test
    fun `unknown rationale keeps the record`() {
        val record = PermissionRecord(requestCount = 1, rationaleSeen = true, lastRationale = true)
        assertEquals(record, record.observe(isGranted = false, shouldShowRationale = null))
    }

    @Test
    fun `rationale seen sticks after it turns false`() {
        val record = PermissionRecord.Empty.observe(false, true).observe(false, false)
        assertEquals(PermissionRecord(rationaleSeen = true, lastRationale = false), record)
    }

    @Test
    fun `combined statuses`() {
        val granted = PermissionStatus.Granted
        assertEquals(granted, emptyList<PermissionStatus>().combined())
        assertEquals(granted, listOf(granted, granted).combined())
        assertEquals(PermissionStatus.NotRequested, listOf(granted, PermissionStatus.NotRequested).combined())
        assertEquals(
            PermissionStatus.Denied(true),
            listOf(PermissionStatus.NotRequested, PermissionStatus.Denied(false), PermissionStatus.Denied(true)).combined(),
        )
        assertEquals(PermissionStatus.Denied(false), listOf(PermissionStatus.NotRequested, PermissionStatus.Denied(false)).combined())
        assertEquals(
            PermissionStatus.PermanentlyDenied,
            listOf(PermissionStatus.Denied(true), PermissionStatus.PermanentlyDenied, granted).combined(),
        )
    }
}
