package io.github.halilozel1903.permissionflow.core

/**
 * Simulates how Android 11+ answers permission requests: the first denial turns the rationale on,
 * the second denial stops the dialog for good ("don't ask again").
 */
class FakePlatform(
    override val sdkInt: Int = 36,
    var hasActivity: Boolean = true,
) : PermissionPlatform {
    val granted = mutableSetOf<String>()
    private val denials = mutableMapOf<String, Int>()
    val blocked = mutableSetOf<String>()

    override fun isGranted(permission: String): Boolean = permission in granted

    override fun shouldShowRationale(permission: String): Boolean? =
        if (!hasActivity) null else permission !in granted && permission !in blocked && denials[permission] == 1

    /** What the system dialog would return; [answer] is the user's choice, `null` to dismiss it. */
    fun request(permissions: List<String>, answer: Boolean?): Map<String, Boolean> =
        permissions.associateWith { permission ->
            val dialogShown = permission !in blocked && (denials[permission] ?: 0) < 2
            when {
                !dialogShown -> false
                answer == true -> true.also { granted += permission; denials.remove(permission) }
                answer == false -> false.also { denials[permission] = (denials[permission] ?: 0) + 1 }
                else -> false
            }
        }

    /** The user revokes the permission in the system settings, which resets the dialog. */
    fun revokeInSettings(permission: String) {
        granted -= permission
        denials.remove(permission)
    }

    /** The user allows the permission in the system settings. */
    fun grantInSettings(permission: String) {
        granted += permission
        denials.remove(permission)
    }
}
