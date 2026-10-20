package li.gkd.app.app

import li.gkd.app.R

import li.gkd.app.model.AppInventory

data class AppCatalogSnapshot(
    val inventory: AppInventory,
    val launcherAppId: String? = null,
    val canQueryPackages: Boolean = true,
    val queryPackagesAbnormal: Boolean = false,
) {
    // 快照不可变：派生集合只算一次，避免列表每项访问时重建整表映射。
    val apps by lazy {
        inventory.othersApps.associateBy { it.id } + inventory.apps.associateBy { it.id }
    }
    val visibleApps by lazy { AppCatalog.visibleApps(apps.values) }
    val systemApps by lazy { apps.filterValues { it.isSystem }.keys }
    val users by lazy { inventory.otherUsers.associateBy { it.id } }
}

/** No snapshot means no successful query yet; refresh errors retain the last complete snapshot. */
data class AppCatalogState(
    val snapshot: AppCatalogSnapshot? = null,
    val refreshing: Boolean = false,
    val failure: Throwable? = null,
)
