package io.github.halilozel1903.permissionflow.core

/**
 * What the app remembers about one runtime permission between launches. Android does not tell apps
 * whether a permission was ever requested, so "permanently denied" can only be told apart from
 * "never asked" with this history.
 *
 * @property requestCount how many permission requests returned a result for this permission since
 * it was last seen granted.
 * @property rationaleSeen whether `shouldShowRequestPermissionRationale` was ever `true` since the
 * permission was last seen granted. Once it has been `true`, going back to `false` while denied
 * means the system stopped showing the dialog.
 * @property lastRationale the last known rationale flag, used when the rationale can't be read
 * (no activity at hand, for example in a ViewModel).
 */
public data class PermissionRecord(
    val requestCount: Int = 0,
    val rationaleSeen: Boolean = false,
    val lastRationale: Boolean = false,
) {
    init {
        require(requestCount >= 0) { "requestCount must not be negative: $requestCount" }
    }

    /** `true` when the app asked for the permission at least once since it was last granted. */
    val wasRequested: Boolean get() = requestCount > 0

    /**
     * The record after reading the permission's current state. A granted permission clears the
     * history: if it is revoked later (in Settings, or a one-time grant expires) the system shows
     * the dialog again, so the app starts over from [PermissionStatus.NotRequested].
     *
     * @param shouldShowRationale `null` when it can't be read (no activity).
     */
    public fun observe(isGranted: Boolean, shouldShowRationale: Boolean?): PermissionRecord = when {
        isGranted -> Empty
        shouldShowRationale == null -> this
        else -> copy(rationaleSeen = rationaleSeen || shouldShowRationale, lastRationale = shouldShowRationale)
    }

    /** The record after a permission request returned a result for this permission. */
    public fun afterRequest(): PermissionRecord =
        copy(requestCount = if (requestCount == Int.MAX_VALUE) requestCount else requestCount + 1)

    public companion object {
        /** A permission the app knows nothing about. */
        public val Empty: PermissionRecord = PermissionRecord()
    }
}

/** Persists a [PermissionRecord] per runtime permission. The Android library keeps them in SharedPreferences. */
public interface PermissionRecordStore {
    /** The record for [permission], or [PermissionRecord.Empty]. */
    public fun read(permission: String): PermissionRecord

    /** Saves the record for [permission]. Writing [PermissionRecord.Empty] may delete it. */
    public fun write(permission: String, record: PermissionRecord)
}

/** A [PermissionRecordStore] that lives in memory, for tests and previews. */
public class InMemoryPermissionRecordStore : PermissionRecordStore {
    private val records = mutableMapOf<String, PermissionRecord>()

    override fun read(permission: String): PermissionRecord =
        synchronized(records) { records[permission] ?: PermissionRecord.Empty }

    override fun write(permission: String, record: PermissionRecord) {
        synchronized(records) {
            if (record == PermissionRecord.Empty) records.remove(permission) else records[permission] = record
        }
    }
}
