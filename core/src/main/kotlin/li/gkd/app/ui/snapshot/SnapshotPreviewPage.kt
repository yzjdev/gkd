package li.gkd.app.ui.snapshot

import li.gkd.app.R

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import li.gkd.app.app.ActivityNames.getShowActivityId
import li.gkd.app.resources.*
import li.gkd.app.state.Loadable
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkAppNameText
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkRetainedSheet
import li.gkd.app.ui.component.LocalEditorWindowInsets
import li.gkd.app.ui.image.appImageLoader
import li.gkd.app.ui.navigation.AppRoute
import li.gkd.app.ui.navigation.HomeRoute
import li.gkd.app.ui.navigation.ImagePreviewItem
import li.gkd.app.ui.navigation.ImagePreviewRoute
import li.gkd.app.ui.navigation.SnapshotPreviewRoute
import li.gkd.app.ui.page.GkImagePreviewContent
import li.gkd.app.ui.platform.GkBackHandler
import li.gkd.app.ui.platform.GkSystemBars
import li.gkd.app.ui.platform.UiHost
import androidx.compose.ui.res.stringResource

@Composable
fun SnapshotPreviewPage(
    host: UiHost,
    route: SnapshotPreviewRoute,
) {
    val mainVm = MainViewModel.requireCurrent()
    val imageLoader = appImageLoader()
    val replaceRoute: (AppRoute) -> Unit = { mainVm.navigator.navigate(it, true) }
    val vm = viewModel { SnapshotViewModel() }
    val shareProgress by vm.shareProgress.collectAsStateWithLifecycle()

    val loadableState by vm.uiState.collectAsStateWithLifecycle()
    val state = loadableState.value
    val catalog by li.gkd.app.app.AppInfoRepository.state.collectAsStateWithLifecycle()
    val nodeSnapshots by vm.nodeSnapshots.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        key(route) {
            val actionRequest by vm.actionRequest.collectAsStateWithLifecycle()
            var imageVersion by remember { mutableIntStateOf(0) }
            var inspectingNodes by remember(route) { mutableStateOf(false) }
            // 顶栏在查看模式下可以收起，所以退出查看还能用返回键。
            GkBackHandler(inspectingNodes) { inspectingNodes = false }
            val snapshotsById = state?.associateBy { it.id }.orEmpty()
            val previewSnapshots = route.snapshotIds.mapNotNull(snapshotsById::get)

            if (previewSnapshots.isNotEmpty()) {
                val initialPage = previewSnapshots.indexOfFirst { it.id == route.snapshotId }
                    .coerceAtLeast(0)
                val pagerState = rememberPagerState(initialPage = initialPage) {
                    previewSnapshots.size
                }
                GkImagePreviewContent(
                    imageLoader = imageLoader, systemBars = { host.GkSystemBars(it) },
                    route = ImagePreviewRoute(items = previewSnapshots.map {
                        ImagePreviewItem(uri = it.image().path)
                    }),
                    pagerState = pagerState,
                    imageVersion = imageVersion,
                    onBack = mainVm.navigator::pop,
                    titleContent = { index ->
                        previewSnapshots.getOrNull(index)?.let { item ->
                            val appName = catalog.snapshot?.apps?.get(item.appId)?.name ?: item.appId
                            val activityId = getShowActivityId(item.appId, item.activityId)
                                ?.takeIf(String::isNotBlank)
                            Column {
                                GkAppNameText(
                                    appId = item.appId,
                                    fallbackName = appName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                )
                                if (activityId != null) {
                                    Text(
                                        text = activityId,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.MiddleEllipsis,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            color = Color.White.copy(alpha = 0.8f),
                                        ),
                                    )
                                }
                            }
                        }
                    },
                    actionContent = { _, index ->
                        val item = previewSnapshots.getOrNull(index)
                        if (item != null) {
                            // 查看模式下顶栏已收起，退出交给返回键，这里只保留进入入口。
                            GkIconButton(
                                imageVector = GkIcons.CenterFocusWeak,
                                contentDescription = stringResource(R.string.snapshot_node_view_enter),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                                onClick = { inspectingNodes = true },
                            )
                            GkIconButton(
                                imageVector = GkIcons.MoreVert,
                                contentDescription = stringResource(R.string.more_label),
                                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White),
                                onClick = { vm.showActions(item.id) },
                            )
                        }
                    },
                    // 进入查看模式就收起顶栏，整块画面都能点到节点；退出靠返回键。
                    hideBars = inspectingNodes,
                    // The snapshot page draws its own zoom-free view so taps can resolve nodes.
                    pageContent = { _, index, onToggleBars ->
                        val item = previewSnapshots.getOrNull(index)
                        if (item != null) {
                            LaunchedEffect(item.id) { vm.ensureNodeSnapshot(item.id) }
                            GkSnapshotNodeView(
                                imageLoader = imageLoader,
                                imagePath = item.image().path,
                                snapshot = nodeSnapshots[item.id],
                                inspecting = inspectingNodes,
                                onToggleBars = onToggleBars,
                            )
                        }
                    },
                )

                val actionSnapshot = actionRequest?.let { request ->
                    previewSnapshots.find { it.id == request.key }
                }
                GkRetainedSheet(
                    request = actionRequest,
                    snapshot = actionSnapshot,
                    missing = actionRequest != null && actionSnapshot == null,
                    onDismissRequest = { request ->
                        vm.dismissActions(request)
                    },
                ) { _, snapshot, sheetState, dismiss ->
                    val routeAfterDelete = if (previewSnapshots.size == 1) {
                        null
                    } else {
                        val currentIndex = previewSnapshots.indexOfFirst { it.id == snapshot.id }
                        val targetIndex = if (currentIndex < previewSnapshots.lastIndex) {
                            currentIndex + 1
                        } else {
                            currentIndex - 1
                        }
                        route.copy(
                            snapshotId = previewSnapshots[targetIndex].id,
                            snapshotIds = route.snapshotIds.filterNot { it == snapshot.id },
                        )
                    }
                    val actions = host.snapshotActions(mainVm,
                        vm,
                        { imageVersion++ },
                        {
                            if (routeAfterDelete == null) {
                                mainVm.navigator.pop()
                            } else {
                                replaceRoute(routeAfterDelete)
                            }
                        },
                        {
                            if (routeAfterDelete == null) {
                                if (mainVm.navigator.topRoute == HomeRoute) mainVm.navigator.navigate(route)
                            } else if (mainVm.navigator.topRoute == routeAfterDelete) {
                                replaceRoute(route)
                            }
                        },
                    )
                    GkSnapshotActionsSheet(
                        imageLoader, snapshot = snapshot,
                        appName = catalog.snapshot?.apps?.get(snapshot.appId)?.name ?: snapshot.appId,
                        sheetState = sheetState,
                        onDismissRequest = dismiss,
                        onShare = { vm.shareSnapshots(listOf(snapshot), host.snapshotPlatformActions()) },
                        shareEnabled = shareProgress == null,
                        onSaveToDownloads = { actions.saveToDownloads(snapshot) },
                        onUpload = { actions.upload(snapshot) },
                        onSaveToAlbum = { actions.saveToAlbum(snapshot) },
                        onReplace = { actions.replace(snapshot) },
                        onDelete = { actions.delete(snapshot) },
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    GkImagePreviewContent(
                        imageLoader = imageLoader,
                        systemBars = { host.GkSystemBars(it) },
                        route = ImagePreviewRoute(title = stringResource(R.string.snapshot_records)),
                        onBack = mainVm.navigator::pop,
                    )
                    if (loadableState is Loadable.Loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        Text(
                            text = (loadableState as? Loadable.Failure)?.cause?.message
                                ?: stringResource(R.string.snapshot_missing_or_deleted),
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White,
                        )
                    }
                }
            }
        }
        shareProgress?.let {
            GkSnapshotShareProgress(
                progress = it,
                onCancel = vm::cancelShare,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(LocalEditorWindowInsets.current ?: WindowInsets.safeDrawing)
                    .then(host.snapshotShareCornerPadding()),
            )
        }
    }
}
