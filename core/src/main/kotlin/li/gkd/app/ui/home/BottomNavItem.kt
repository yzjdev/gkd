package li.gkd.app.ui.home

import li.gkd.app.R

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import li.gkd.app.resources.*
import li.gkd.app.ui.component.GkIcons
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

sealed class BottomNavItem(
    val key: Int,
    val labelResource: Int,
    val icon: ImageVector,
) {
    val label: String @Composable get() = stringResource(labelResource)

    object Dashboard : BottomNavItem(
        key = 0,
        labelResource = R.string.home_title,
        icon = GkIcons.Home,
    )

    object SubsManage : BottomNavItem(
        key = 1,
        labelResource = R.string.subscription_title,
        icon = GkIcons.StackedDocuments,
    )

    object AppList : BottomNavItem(
        key = 2,
        labelResource = R.string.apps_title,
        icon = GkIcons.Android,
    )

    object Snapshot : BottomNavItem(
        key = 4,
        labelResource = R.string.snapshot_title,
        icon = GkIcons.Image,
    )

    object Settings : BottomNavItem(
        key = 3,
        labelResource = R.string.settings_title,
        icon = GkIcons.Settings,
    )

    companion object {
        val allSubObjects by lazy { arrayOf(Dashboard, SubsManage, AppList, Snapshot, Settings) }
    }
}
