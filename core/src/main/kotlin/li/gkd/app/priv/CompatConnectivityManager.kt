package li.gkd.app.priv

import li.gkd.app.R

import android.content.Context
import li.gkd.app.util.shellCommand
import priv.kit.core.binder.PrivilegeBinderWrapper

/**
 * Per-package network control through the OEM deny chain (FIREWALL_CHAIN_OEM_DENY_3).
 * Available from Android 13; rules live in a BPF map and are dropped on reboot.
 */
class CompatConnectivityManager {
    private val binder: PrivilegeBinderWrapper = requireNotNull(
        PrivilegeBinderWrapper.fromSystemService(Context.CONNECTIVITY_SERVICE),
    )

    /** The deny chain must be enabled before any package deny bit takes effect. */
    fun setChainEnabled(enabled: Boolean) {
        val result = binder.shellCommand("set-chain3-enabled", enabled.toString())
        check(result.ok) { result.error ?: "set-chain3-enabled rejected" }
    }

    fun setPackageNetworkingEnabled(packageName: String, enabled: Boolean) {
        val result = binder.shellCommand(
            "set-package-networking-enabled",
            enabled.toString(),
            packageName,
        )
        check(result.ok) { result.error ?: "set-package-networking-enabled rejected" }
    }

    /** null means the service did not report a readable deny-bit state. */
    fun isPackageNetworkingEnabled(packageName: String): Boolean? {
        val result = binder.shellCommand("get-package-networking-enabled", packageName)
        if (!result.ok) return null
        return when {
            result.result.trim().endsWith(":allow") -> true
            result.result.trim().endsWith(":deny") -> false
            else -> null
        }
    }
}
