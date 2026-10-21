package li.gkd.app.ui.home

import li.gkd.app.R

import li.gkd.app.ui.component.GkTriggerCount
import li.gkd.app.record.TriggerSummary
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import li.gkd.app.ui.component.GkPullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.key.Key
import li.gkd.app.ui.component.gkTopAppBarScroll
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick as semanticsOnLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import li.gkd.app.app.AppInfoRepository
import li.gkd.app.priv.appNetworkControlSupported
import li.gkd.app.priv.privilegeContextFlow
import li.gkd.app.priv.readAppNetworkingEnabled
import li.gkd.app.priv.setAppNetworkingEnabled
import li.gkd.app.error.displayMessage
import li.gkd.app.model.AppInfo
import li.gkd.app.permission.AppPermission
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.state.Loadable
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkAnimatedFloatingActionButton
import li.gkd.app.ui.component.GkAppBarTextField
import li.gkd.app.ui.component.GkAppNameText
import li.gkd.app.ui.component.GkCheckbox
import li.gkd.app.ui.component.GkDesktopKeyHandler
import li.gkd.app.ui.component.GkEmptyState
import li.gkd.app.ui.component.GkFilterIconButton
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkMenuGroupCard
import li.gkd.app.ui.component.GkMenuItemCheckbox
import li.gkd.app.ui.component.GkMenuItemRadioButton
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkPageBottomSpaceDefaults
import li.gkd.app.ui.component.GkQueryPkgAuthCard
import li.gkd.app.ui.component.GkRuleStats
import li.gkd.app.ui.component.GkRuleStatsData
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.ui.component.GkModalBottomSheet
import li.gkd.app.ui.component.GkSwitch
import li.gkd.app.ui.component.LocalOverlayBackHandler
import li.gkd.app.ui.component.autoFocus
import li.gkd.app.ui.component.rememberListScrollState
import li.gkd.app.ui.icon.GkBlockCloseIconButton
import li.gkd.app.ui.icon.GkSearchCloseIconButton
import li.gkd.app.ui.component.GkAppIcon
import li.gkd.app.ui.navigation.AppConfigRoute
import li.gkd.app.ui.navigation.EditBlockAppListRoute
import li.gkd.app.ui.option.AppGroupOption
import li.gkd.app.app.AppSort
import li.gkd.app.model.findOption
import li.gkd.app.ui.platform.GkBackHandler
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.platform.hideIme
import li.gkd.app.ui.share.launchUiAction
import li.gkd.app.ui.style.getUpDownTransform
import li.gkd.app.ui.style.itemPadding
import li.gkd.app.util.ToastUtils
import androidx.compose.ui.res.stringResource

data class AppListContentState(
    val appInfos: List<AppInfo>,
    val listState: Loadable<Unit> = Loadable.Ready(Unit),
    val ruleStatsFailed: Boolean = false,
    val triggerStats: li.gkd.app.record.TriggerStats? = null,
    val searchText: String = "",
    val showSearchBar: Boolean = false,
    val editWhiteListMode: Boolean = false,
    val showAllApps: Boolean = true,
    val ruleStats: Map<String, GkRuleStatsData> = emptyMap(),
    val whiteListAppIds: Set<String> = emptySet(),
    val canQueryPackages: Boolean = true,
    val queryPackagesAbnormal: Boolean = false,
    val refreshing: Boolean = false,
    val sort: Int = 0,
    val group: Int = 3,
    val showBlocked: Boolean = true,
)

@Composable
fun appListPage(
    host: UiHost,
    vm: HomeViewModel,
): ScaffoldExt {
    val mainVm = MainViewModel.requireCurrent()
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val appState by vm.appsState.collectAsStateWithLifecycle()
    val blocked by SettingsRepository.blockMatchAppList.collectAsStateWithLifecycle()
    val networkDisabledAppIds by SettingsRepository.networkDisabledAppList.collectAsStateWithLifecycle()
    val controls by vm.appListState.collectAsStateWithLifecycle()
    // 筛选/排序是整表扫描，只在数据源或设置变化时重算，重组时直接复用。
    val state = remember(controls, appState, store, blocked) {
        controls.content(appState, store, blocked)
    }

    val scope = rememberCoroutineScope()
    // 长按弹出的联网菜单目标；开关以持久化禁网列表为状态来源，打开菜单时用实际状态校准。
    var networkTarget by remember { mutableStateOf<AppInfo?>(null) }
    fun requestNetworkMenu(appInfo: AppInfo) {
        if (!appNetworkControlSupported) {
            ToastUtils.show(R.string.app_network_unsupported.getSync(), forced = true)
            return
        }
        if (privilegeContextFlow.value == null) {
            ToastUtils.show(R.string.app_network_privilege_required.getSync(), forced = true)
            return
        }
        scope.launch {
            val enabled = readAppNetworkingEnabled(appInfo.id)
            if (enabled == null) {
                ToastUtils.show(R.string.app_network_privilege_required.getSync(), forced = true)
                return@launch
            }
            // 禁网规则在重启或拒绝链被关闭后会静默失效，实际状态可能与缓存不一致；
            // 以服务回报的实际状态为准回写，开关才不会停留在失效前的旧状态。
            val blocked = !enabled
            if (blocked != (appInfo.id in SettingsRepository.networkDisabledAppList.value)) {
                SettingsRepository.updateNetworkDisabledAppList { ids ->
                    if (blocked) ids + appInfo.id else ids - appInfo.id
                }
            }
            networkTarget = appInfo
        }
    }
    fun setNetworkEnabled(appInfo: AppInfo, enabled: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                setAppNetworkingEnabled(appInfo.id, enabled)
                SettingsRepository.updateNetworkDisabledAppList { ids ->
                    if (enabled) ids - appInfo.id else ids + appInfo.id
                }
                ToastUtils.show(
                    getString(
                        if (enabled) R.string.app_network_enable_success
                        else R.string.app_network_disable_success,
                        appInfo.name,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ToastUtils.show(
                    getString(
                        if (enabled) R.string.app_network_enable_failed
                        else R.string.app_network_disable_failed,
                        e.displayMessage(),
                    ),
                    forced = true,
                )
            }
        }
    }
    fun refresh() {
        scope.launch {
            try {
                AppInfoRepository.refresh()
                ToastUtils.show(getString(R.string.app_list_update_success))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ToastUtils.show(e.displayMessage())
            }
        }
    }
    GkDesktopKeyHandler(Key.F5, onKey = ::refresh)
    val appInfos = state.appInfos
    val searchStr = state.searchText

    val showSearchBar = state.showSearchBar
    val refreshing = state.refreshing
    val editWhiteListMode = state.editWhiteListMode
    val pageScrollState = rememberListScrollState()
    val scrollBehavior = pageScrollState.scrollBehavior
    val listState = pageScrollState.listState
    pageScrollState.ResetOnListChange(
        appInfos,
        key = { it.id },
        leadingItemKey = if (state.canQueryPackages) null else 1,
    )
    ResetPageScrollOnRequest(
        vm.homeState,
        BottomNavItem.AppList,
        pageScrollState::resetScrollAndAwait,
    )
    return ScaffoldExt(
        navItem = BottomNavItem.AppList,
        modifier = Modifier.gkTopAppBarScroll(scrollBehavior),
        topBar = {
            DisposableEffect(null) {
                onDispose {
                    vm.leaveAppList()
                }
            }
            GkTopAppBar(
                scrollBehavior = scrollBehavior,
                title = {
                    val firstShowSearchBar = remember { showSearchBar }
                    if (showSearchBar) {
                        GkBackHandler { if (!host.hideIme()) vm.closeAppSearch() }
                        GkAppBarTextField(
                            value = searchStr,
                            onValueChange = vm::setAppSearchText,
                            hint = stringResource(R.string.app_name_id_input_hint),
                            modifier = if (firstShowSearchBar) Modifier else Modifier.autoFocus(),
                        )
                    } else {
                        val titleModifier =
                            Modifier.clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {
                                    pageScrollState.resetScroll()
                                },
                            )
                        if (editWhiteListMode) {
                            GkBackHandler(onBack = vm::closeAppListEdit)
                        }
                        AnimatedContent(
                            targetState = editWhiteListMode,
                            transitionSpec = { getUpDownTransform() },
                        ) { localEditWhiteListMode ->
                            if (localEditWhiteListMode) {
                                Text(
                                    modifier = titleModifier,
                                    text = stringResource(R.string.app_whitelist),
                                )
                            } else {
                                Text(
                                    modifier = titleModifier,
                                    text = BottomNavItem.AppList.label,
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (state.queryPackagesAbnormal) {
                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.error
                        ) {
                            GkIconButton(
                                imageVector = GkIcons.WarningAmber,
                                contentDescription = stringResource(R.string.permission_error),
                                onClick = {
                                    scope.launch {
                                        mainVm.dialogRequests.showMessage(
                                            getString(R.string.permission_error),
                                            getString(
                                                R.string.app_list_permission_error_description,
                                                getString(R.string.permission_query_apps),
                                            ),
                                        )
                                    }
                                },
                            )
                        }
                    }
                    GkSearchCloseIconButton(
                        onClick = vm::toggleAppSearch,
                        isSearchOpen = showSearchBar,
                        contentDescription =
                            if (showSearchBar) stringResource(R.string.search_close)
                            else stringResource(R.string.app_list_search),
                    )
                    var expanded by remember { mutableStateOf(false) }
                    if (expanded) LocalOverlayBackHandler.current { expanded = false }
                    GkFilterIconButton(
                        filtered = !state.showAllApps,
                        contentDescription = stringResource(R.string.sort_filter),
                        onClick = {
                            expanded = true
                        },
                    )
                    Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            GkMenuGroupCard(
                                inTop = true,
                                title = stringResource(R.string.sort_title),
                            ) {
                                AppSort.options.forEach { option ->
                                    GkMenuItemRadioButton(
                                        text = stringResource(option.labelResource),
                                        selected =
                                            AppSort.options.findOption(state.sort) == option,
                                        onClick = { SettingsRepository.updateSettings { it.copy(appSort = option.value) } },
                                    )
                                }
                            }
                            GkMenuGroupCard(title = stringResource(R.string.group_title)) {
                                AppGroupOption.normalObjects.forEach { option ->
                                    val newValue = option.invert(state.group)
                                    GkMenuItemCheckbox(
                                        enabled = newValue != 0,
                                        text = stringResource(option.labelResource),
                                        checked = option.include(state.group),
                                        onClick = { SettingsRepository.updateSettings { it.copy(appGroupType = newValue) } },
                                    )
                                }
                            }
                            GkMenuGroupCard(title = stringResource(R.string.filter_title)) {
                                GkMenuItemCheckbox(
                                    text = stringResource(R.string.whitelist_title),
                                    checked = state.showBlocked,
                                    onClick = {
                                        SettingsRepository.updateSettings { it.copy(showBlockApp = !state.showBlocked) }
                                    },
                                )
                            }
                        }
                    }
                    GkBlockCloseIconButton(
                        isClose = editWhiteListMode,
                        contentDescription = stringResource(R.string.whitelist_edit_mode_toggle),
                        onClickLabel =
                            if (editWhiteListMode) stringResource(R.string.edit_exit)
                            else stringResource(R.string.edit_enter),
                        onClick = { vm.toggleAppListEdit(blocked) },
                    )
                },
            )
        },
        floatingActionButton = {
            GkAnimatedFloatingActionButton(
                visible = editWhiteListMode,
                contentDescription = stringResource(R.string.whitelist_edit),
                onClick = {
                    mainVm.navigator.navigate(EditBlockAppListRoute)
                },
                imageVector = GkIcons.Edit,
            )
        },
    ) { contentPadding ->
        networkTarget?.let { target ->
            GkModalBottomSheet(
                onDismissRequest = { networkTarget = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GkAppIcon(target.id, 32.dp)
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_network_disable),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = stringResource(R.string.app_network_clear_on_reboot),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        GkSwitch(
                            checked = target.id in networkDisabledAppIds,
                            onCheckedChange = { setNetworkEnabled(target, !it) },
                        )
                    }
                }
            }
        }
        GkPullToRefreshBox(
            modifier = Modifier.padding(contentPadding),
            isRefreshing = refreshing,
            onRefresh = ::refresh,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                if (!state.canQueryPackages) {
                    item(key = 1, contentType = 1) {
                        GkQueryPkgAuthCard(state.refreshing, mainVm.scope.launchUiAction {
                            mainVm.permissions.ensurePermissions(AppPermission.QueryPackages)
                        })
                    }
                }
                if (state.listState !is Loadable.Ready || state.ruleStatsFailed) {
                    item("load-state") {
                        Text(
                            stringResource(
                                if (state.listState is Loadable.Loading) R.string.loading_progress
                                else R.string.data_load_failed
                            ),
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
                items(appInfos, { it.id }) { appInfo ->
                    val stats = if (editWhiteListMode) null else state.ruleStats[appInfo.id]
                    GkAppListItem(
                        appInfo = appInfo,
                        icon = { GkAppIcon(appInfo.id, 32.dp) },
                        name = { GkAppNameText(appId = appInfo.id, appInfo = appInfo) },
                        stats = stats,
                        triggerSummary = if (editWhiteListMode) null else state.triggerStats?.app(appInfo.id),
                        editWhiteListMode = editWhiteListMode,
                        inWhiteList = appInfo.id in state.whiteListAppIds,
                        networkingDisabled = appInfo.id in networkDisabledAppIds,
                        onLongClick = if (editWhiteListMode) null else {
                            { requestNetworkMenu(appInfo) }
                        },
                        onClick = {
                            if (editWhiteListMode) {
                                SettingsRepository.updateBlockMatchAppList {
                                    if (appInfo.id in it) it - appInfo.id else it + appInfo.id
                                }
                            } else {
                                host.hideIme()
                                mainVm.navigator.navigate(AppConfigRoute(appInfo.id))
                            }
                        },
                    )
                }
                item("placeholder", "placeholder") {
                    if (
                        state.listState is Loadable.Ready &&
                            appInfos.isEmpty() &&
                            searchStr.isNotEmpty()
                    ) {
                        GkEmptyState(
                            text =
                                if (state.showAllApps) stringResource(R.string.search_no_results)
                                else stringResource(R.string.search_no_results_filter_hint)
                        )
                        GkPageBottomSpace(height = GkPageBottomSpaceDefaults.CompactHeight)
                    } else {
                        GkPageBottomSpace()
                    }
                }
            }
        }
    }
}

@Composable
fun GkAppListItem(
    appInfo: AppInfo,
    icon: @Composable () -> Unit,
    name: @Composable () -> Unit,
    stats: GkRuleStatsData?,
    triggerSummary: TriggerSummary? = null,
    editWhiteListMode: Boolean,
    inWhiteList: Boolean,
    onClick: () -> Unit,
    networkingDisabled: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val count = triggerSummary?.count?.takeIf { it > 0 && !editWhiteListMode }
    val countText = count?.toString()
    val statsDescription = stats?.takeIf { it.hasRules }?.description
    val appIdText = appInfo.id.takeIf { it != appInfo.name }
    val supportingDescription = listOfNotNull(
        statsDescription ?: appIdText,
        countText?.let { stringResource(R.string.trigger_count_value, it) },
    ).joinToString("，")
    val description =
        if (editWhiteListMode || supportingDescription.isEmpty()) appInfo.name
        else
            stringResource(
                R.string.app_whitelist_state_description,
                appInfo.name,
                supportingDescription,
            )
    val member = stringResource(R.string.whitelist_member)
    val notMember = stringResource(R.string.whitelist_not_member)
    val clickLabel =
        if (editWhiteListMode)
            stringResource(
                if (inWhiteList) R.string.whitelist_remove else R.string.whitelist_add
            )
        else stringResource(R.string.rule_summary_open)
    val longClickLabel = stringResource(
        if (networkingDisabled) R.string.app_network_enable else R.string.app_network_disable
    )
    val networkingDisabledDescription = stringResource(R.string.app_network_disabled_state)

    Row(
        modifier =
            Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (inWhiteList) {
                        stateDescription = member
                    } else if (editWhiteListMode) {
                        stateDescription = notMember
                    }
                    onClick(
                        label = clickLabel,
                        action = null,
                    )
                    if (onLongClick != null) {
                        semanticsOnLongClick(
                            label = longClickLabel,
                            action = null,
                        )
                    }
                }
                .itemPadding()
                .alpha(if (inWhiteList && !editWhiteListMode) 0.65f else 1f),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            name()
            if (stats != null) {
                GkRuleStats(stats, emptyText = appIdText.orEmpty())
            } else if (appIdText != null) {
                Text(
                    text = appIdText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )
            }
        }
        if (editWhiteListMode) {
            GkCheckbox(
                key = appInfo.id,
                checked = inWhiteList,
            )
        } else if (countText != null || inWhiteList || networkingDisabled) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (count != null) GkTriggerCount(count, lastTriggerTime = triggerSummary.lastTime)
                if (networkingDisabled) {
                    GkIcon(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(20.dp)
                            .semantics { contentDescription = networkingDisabledDescription },
                        imageVector = GkIcons.WifiOff,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                if (inWhiteList) {
                    GkIcon(
                        modifier = Modifier.padding(2.dp).size(20.dp),
                        imageVector = GkIcons.Block,
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}
