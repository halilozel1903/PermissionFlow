package io.github.halilozel1903.permissionflow.core

/**
 * The state machine that turns what Android reports about a runtime permission, plus the app's
 * [PermissionRecord], into a [PermissionStatus].
 *
 * | Granted | Rationale | Record | Status |
 * | --- | --- | --- | --- |
 * | yes | any | any | `Granted` |
 * | no | `true` | any | `Denied(shouldShowRationale = true)` |
 * | no | `false` | never requested | `NotRequested` |
 * | no | `false` | rationale was `true` before | `PermanentlyDenied` |
 * | no | `false` | requested twice without ever seeing a rationale | `PermanentlyDenied` |
 * | no | `false` | requested once, no rationale yet | `Denied(shouldShowRationale = false)` |
 *
 * The last row covers a dialog the user dismissed without answering (tapping outside it on Android
 * 11+), after which the system asks again. When the rationale can't be read (`null`), the record's
 * last known value is used instead.
 */
public object PermissionStatusResolver {

    /**
     * @param isGranted whether the permission is granted right now (`checkSelfPermission`).
     * @param shouldShowRationale `shouldShowRequestPermissionRationale`, or `null` when there is no
     * activity to ask.
     * @param record what the app remembers about the permission, already updated with
     * [PermissionRecord.observe].
     */
    public fun resolve(
        isGranted: Boolean,
        shouldShowRationale: Boolean?,
        record: PermissionRecord,
    ): PermissionStatus {
        val rationale = shouldShowRationale ?: record.lastRationale
        return when {
            isGranted -> PermissionStatus.Granted
            rationale -> PermissionStatus.Denied(shouldShowRationale = true)
            !record.wasRequested -> PermissionStatus.NotRequested
            record.rationaleSeen || record.requestCount >= PERMANENT_AFTER_REQUESTS -> PermissionStatus.PermanentlyDenied
            else -> PermissionStatus.Denied(shouldShowRationale = false)
        }
    }

    /**
     * How many requests without any rationale it takes to call a permission permanently denied. The
     * system shows a rationale after the first real denial, so two silent denials mean the dialog is
     * no longer shown (or the permission is blocked by policy or missing from the manifest).
     */
    public const val PERMANENT_AFTER_REQUESTS: Int = 2
}
