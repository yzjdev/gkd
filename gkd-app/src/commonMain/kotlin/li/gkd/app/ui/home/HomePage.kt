package li.gkd.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.snapshot.snapshotPage

@Composable
fun HomePage() {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { HomeViewModel() }
    DisposableEffect(vm, mainVm.homeNavigation) {
        mainVm.homeNavigation.bind(vm.homeState)
        onDispose { mainVm.homeNavigation.unbind(vm.homeState) }
    }
    GkHomeHost(vm.homeState) { tab ->
        when (tab) {
            BottomNavItem.Dashboard -> dashboardPage(vm)
            BottomNavItem.AppList -> appListPage(vm)
            BottomNavItem.Snapshot -> snapshotPage(homeState = vm.homeState)
            BottomNavItem.SubsManage -> subsManagePage(vm)
            BottomNavItem.Settings -> settingsPage(vm)
        }
    }
}
