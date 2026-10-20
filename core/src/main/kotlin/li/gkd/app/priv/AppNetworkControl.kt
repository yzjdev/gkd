package li.gkd.app.priv

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import li.gkd.app.util.AndroidTarget

/** Per-package network control relies on the OEM deny chain added in Android 13. */
val appNetworkControlSupported: Boolean get() = AndroidTarget.TIRAMISU

/** True means networking is allowed; null means the privilege service could not report it. */
suspend fun readAppNetworkingEnabled(appId: String): Boolean? = withContext(Dispatchers.IO) {
    privilegeContextFlow.value?.isPackageNetworkingEnabled(appId)
}

/** Throws when the privilege service is absent or rejects the change. */
suspend fun setAppNetworkingEnabled(appId: String, enabled: Boolean) {
    withContext(Dispatchers.IO) {
        val context = checkNotNull(privilegeContextFlow.value) {
            "Privilege service is not connected"
        }
        context.setPackageNetworkingEnabled(appId, enabled)
    }
}
