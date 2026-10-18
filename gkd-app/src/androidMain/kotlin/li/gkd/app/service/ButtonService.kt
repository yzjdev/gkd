package li.gkd.app.service

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import li.gkd.app.notif.NotificationCatalog
import li.gkd.app.resources.*
import li.gkd.app.snapshot.SnapshotCaptureHost
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.share.launchUi
import li.gkd.app.util.IntentUtils
import li.gkd.app.util.ToastUtils

class ButtonService : OverlayWindowService(
    positionKey = "button"
) {
    // The snapshot button lives in the accessibility overlay, so it needs no draw-over-apps grant.
    override fun resolveOverlayWindowHost(): OverlayWindowService.OverlayWindowHost? {
        val service = A11yService.instance ?: return null
        return OverlayWindowService.OverlayWindowHost(
            service.wm,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        )
    }

    override fun onOverlayWindowHostUnavailable() {
        // start() 已在无障碍未连接时提示原因，这里只收尾。
        stopSelf()
    }

    override fun onClickView() {
        lifecycleScope.launchUi {
            SnapshotCaptureHost.capture()
        }
    }

    override fun onLongClickView() = stopSelf()

    @Composable
    override fun ComposeContent() {
        val alpha = 0.75f
        GkIcon(
            imageVector = GkIcons.CenterFocusWeak,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha))
                .size(40.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
        )
    }

    init {
        useServicePresence(
            stateFlow = isRunning,
            name = Res.string.snapshot_button_service.getSync(),
        )
        onCreated {
            NotificationCatalog.button().startForeground()
            // The overlay belongs to the accessibility window; it cannot outlive that connection.
            lifecycleScope.launch {
                A11yService.isRunning.first { !it }
                stopSelf()
            }
        }
    }

    companion object {
        val isRunning: StateFlow<Boolean>
            field = MutableStateFlow(false)

        fun start() {
            // 无障碍类型悬浮窗由无障碍服务提供窗口令牌，未连接时无法显示。
            if (A11yService.instance == null) {
                ToastUtils.show(Res.string.service_not_connected.getSync())
                return
            }
            IntentUtils.startForegroundService(ButtonService::class)
        }

        fun stop() = IntentUtils.stopService(ButtonService::class)

    }
}
