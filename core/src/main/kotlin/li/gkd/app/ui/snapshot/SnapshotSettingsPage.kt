package li.gkd.app.ui.snapshot

import li.gkd.app.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import li.gkd.app.ui.component.gkTopAppBarScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.network.AppLinks
import li.gkd.app.resources.*
import li.gkd.app.settings.ScreenshotConfigResult
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkOutlinedTextField
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkScaffold
import li.gkd.app.ui.component.GkSizedIconButton
import li.gkd.app.ui.component.GkTextSwitch
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.ui.component.autoFocus
import li.gkd.app.ui.platform.UiHost
import li.gkd.app.ui.share.launchUi
import li.gkd.app.ui.style.titleItemPadding
import li.gkd.app.util.ToastUtils
import androidx.compose.ui.res.stringResource

@Composable
fun SnapshotSettingsPage(
    host: UiHost,
) {
    val mainVm = MainViewModel.requireCurrent()
    val vm = viewModel { SnapshotSettingsViewModel() }
    val scope = rememberCoroutineScope()
    val store by SettingsRepository.settings.collectAsStateWithLifecycle()
    val screenshotServiceRunning: Boolean = screenshotServiceRunning()

    val nativeScreenshotAvailable: Boolean = nativeScreenshotAvailable()

    val showCaptureScreenshotDialog by vm.showCaptureScreenshotDialog.collectAsStateWithLifecycle()

    val appIdDraft by vm.appIdDraft.collectAsStateWithLifecycle()
    val eventSelectorDraft by vm.eventSelectorDraft.collectAsStateWithLifecycle()
    if (showCaptureScreenshotDialog) {
        CaptureScreenshotConfigDialog(
            appIdValue = appIdDraft,
            eventSelectorValue = eventSelectorDraft,
            onAppIdChange = vm::setAppIdDraft,
            onEventSelectorChange = vm::setEventSelectorDraft,
            onOpenHelp = {
                vm.closeCaptureScreenshotDialog()
                mainVm.navigator.openWebPage(AppLinks.SnapshotHelp)
            },
            onDismissRequest = { vm.closeCaptureScreenshotDialog() },
            onConfirm = { appId, selector ->
                when (SettingsRepository.saveCaptureScreenshotConfig(appId, selector)) {
                    ScreenshotConfigResult.Unchanged -> vm.closeCaptureScreenshotDialog()
                    ScreenshotConfigResult.Saved -> {
                        ToastUtils.show(R.string.update_success.getSync())
                        vm.closeCaptureScreenshotDialog()
                    }
                    ScreenshotConfigResult.InvalidApp -> ToastUtils.show(R.string.app_id_invalid.getSync())
                    ScreenshotConfigResult.InvalidSelector ->
                        ToastUtils.show(R.string.event_selector_invalid.getSync())
                }
            },
        )
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    GkScaffold(
        modifier = Modifier.gkTopAppBarScroll(scrollBehavior),
        topBar = {
            GkTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    GkIconButton(
                        imageVector = GkIcons.ArrowBack,
                        onClick = mainVm.navigator::pop,
                    )
                },
                title = { Text(text = stringResource(R.string.snapshot_settings)) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(contentPadding)
        ) {
            Text(
                text = stringResource(R.string.snapshot_capture_methods),
                modifier = Modifier.titleItemPadding(showTop = false),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            if (!nativeScreenshotAvailable) {
                GkTextSwitch(
                    title = stringResource(R.string.screenshot_service),
                    subtitle = stringResource(R.string.screenshot_service_description),
                    checked = screenshotServiceRunning,
                    onCheckedChange = host::setScreenshotServiceEnabled,
                )
            }
            GkTextSwitch(
                title = stringResource(R.string.snapshot_volume_trigger),
                subtitle = stringResource(R.string.snapshot_volume_trigger_description),
                checked = store.captureVolumeChange,
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings {
                        it.copy(captureVolumeChange = enabled)
                    }
                },
            )
            GkTextSwitch(
                title = stringResource(R.string.snapshot_screenshot_trigger),
                subtitle = stringResource(R.string.snapshot_screenshot_trigger_description),
                checked = store.captureScreenshot,
                suffixIcon = {
                    GkSizedIconButton(
                        size = 32.dp,
                        iconSize = 20.dp,
                        onClickLabel = stringResource(R.string.snapshot_screenshot_settings_open),
                        onClick = { vm.editCaptureScreenshot(store.screenshotTargetAppId, store.screenshotEventSelector) },
                        imageVector = GkIcons.PageInfo,
                        contentDescription =
                            stringResource(R.string.snapshot_screenshot_settings),
                    )
                },
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings { it.copy(captureScreenshot = enabled) }
                    val current = SettingsRepository.settings.value
                    if (
                        enabled &&
                            (current.screenshotTargetAppId.isEmpty() ||
                                current.screenshotEventSelector.isEmpty())
                    ) {
                        ToastUtils.show(R.string.snapshot_trigger_config_required.getSync())
                    }
                },
            )

            Text(
                text = stringResource(R.string.screenshot_processing),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            GkTextSwitch(
                title = stringResource(R.string.snapshot_hide_status_bar),
                subtitle = stringResource(R.string.snapshot_hide_status_bar_description),
                checked = store.hideSnapshotStatusBar,
                onCheckedChange = { enabled ->
                    SettingsRepository.updateSettings {
                        it.copy(hideSnapshotStatusBar = enabled)
                    }
                },
            )

            Text(
                text = stringResource(R.string.action_export),
                modifier = Modifier.titleItemPadding(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            GkTextSwitch(
                title = stringResource(R.string.snapshot_auto_export),
                subtitle = stringResource(R.string.snapshot_auto_export_description),
                checked = store.autoSaveSnapshotToDownloads,
                onCheckedChange = { enabled ->
                    scope.launchUi {
                        if (!enabled || host.ensureSnapshotSavePermission())
                            SettingsRepository.updateSettings {
                                it.copy(autoSaveSnapshotToDownloads = enabled)
                            }
                    }
                },
            )
            GkPageBottomSpace()
        }
    }
}

@Composable
private fun CaptureScreenshotConfigDialog(
    appIdValue: String,
    eventSelectorValue: String,
    onAppIdChange: (String) -> Unit,
    onEventSelectorChange: (String) -> Unit,
    onOpenHelp: () -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    GkAlertDialog(
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.snapshot_screenshot_trigger))
                GkIconButton(
                    imageVector = GkIcons.HelpOutline,
                    onClick = onOpenHelp,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                GkOutlinedTextField(
                    label = { Text(stringResource(R.string.app_id)) },
                    value = appIdValue,
                    placeholder = { Text(text = stringResource(R.string.target_app_id_hint)) },
                    onValueChange = onAppIdChange,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                GkOutlinedTextField(
                    label = { Text(stringResource(R.string.snapshot_event_selector)) },
                    value = eventSelectorValue,
                    placeholder = {
                        Text(text = stringResource(R.string.snapshot_event_selector_hint))
                    },
                    onValueChange = onEventSelectorChange,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().autoFocus(),
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = { onConfirm(appIdValue, eventSelectorValue) }) {
                Text(text = stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}
