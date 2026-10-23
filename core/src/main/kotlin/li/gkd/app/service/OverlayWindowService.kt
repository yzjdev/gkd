package li.gkd.app.service

import li.gkd.app.R

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import li.gkd.app.a11y.topActivityFlow
import li.gkd.app.app
import li.gkd.app.permission.PermissionStates
import li.gkd.app.resources.*
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.icon.DragPan
import li.gkd.app.ui.style.AppTheme
import li.gkd.app.ui.style.iconTextSize
import li.gkd.app.util.BarUtils
import li.gkd.app.util.ScreenUtils
import li.gkd.app.util.ThreadUtils
import li.gkd.app.util.TimeUtils
import li.gkd.app.util.ToastUtils
import li.gkd.app.util.px
import androidx.compose.ui.res.stringResource
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds

private var tempShareContext: ShareContext? = null
private fun acquireShareContext(): ShareContextLease {
    val shareContext = tempShareContext ?: ShareContext().apply { tempShareContext = this }
    shareContext.count++
    return ShareContextLease(shareContext)
}

private class ShareContextLease(val context: ShareContext) : AutoCloseable {
    override fun close() {
        context.count--
        if (context.count == 0) {
            context.scope.cancel()
            tempShareContext = null
        }
    }
}

private class ShareContext {
    var count = 0
    val scope = MainScope()

    init {
        scope.launch {
            var canDrawOverlays = PermissionStates.drawOverlays.updateAndGet()
            topActivityFlow
                .map { it.appId to it.activityId }
                .distinctUntilChanged()
                .collectLatest {
                    var i = 0
                    while (i < 6 && isActive) {
                        val oldV = canDrawOverlays
                        val newV = PermissionStates.drawOverlays.updateAndGet()
                        canDrawOverlays = newV
                        if (!newV && oldV) {
                            ToastUtils.show(R.string.overlay_screen_denied.getSync())
                            break
                        }
                        delay(500.milliseconds)
                        i++
                    }
                }
        }
    }
}

abstract class OverlayWindowService(
    private val positionKey: String,
) : LifecycleHookService(), SavedStateRegistryOwner {

    companion object {
        private var aliveSize = 0
        val isAnyAlive: Boolean
            get() = aliveSize > 0
    }

    private val resizeFlow = MutableSharedFlow<Unit>()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch { resizeFlow.emit(Unit) }
    }

    private val registryController = SavedStateRegistryController.create(this).apply {
        performAttach()
        performRestore(null)
    }
    override val savedStateRegistry get() = registryController.savedStateRegistry

    @Composable
    abstract fun ComposeContent()

    /** The window this service attaches to; null means the required host is unavailable. */
    protected class OverlayWindowHost(
        val windowManager: WindowManager,
        val type: Int,
    )

    protected open fun resolveOverlayWindowHost(): OverlayWindowHost? =
        OverlayWindowHost(app.windowManager, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)

    protected open fun onOverlayWindowHostUnavailable() {
        stopSelf()
    }

    @Composable
    fun ClosableTitle(
        title: String,
        onMinimizeRequest: (() -> Unit)? = null,
        minimizeContentDescription: String = stringResource(R.string.window_minimize),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            GkIcon(imageVector = DragPan, modifier = Modifier.iconTextSize())
            Text(text = title, modifier = Modifier.weight(1f))
            if (onMinimizeRequest != null) {
                GkIcon(
                    imageVector = GkIcons.CollapseContent,
                    contentDescription = minimizeContentDescription,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraSmall)
                        .clickable(onClick = TimeUtils.throttle(onMinimizeRequest))
                        .iconTextSize(),
                )
            }
            GkIcon(
                imageVector = GkIcons.Close,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .clickable(onClick = TimeUtils.throttle {
                        stopSelf()
                    })
                    .iconTextSize()
            )
        }
    }

    open fun onClickView() {}

    open fun isViewClickEnabled(): Boolean = true

    open fun onLongClickView() {}

    val view by lazy {
        ComposeView(this).apply {
            setViewTreeSavedStateRegistryOwner(this@OverlayWindowService)
            setViewTreeLifecycleOwner(this@OverlayWindowService)
            setContent {
                AppTheme(invertedTheme = true) {
                    Box {
                        ComposeContent()
                    }
                }
            }
        }
    }

    private val minMargin get() = 10.dp.px.toInt()
    private val defaultPosition get() = listOf(minMargin, BarUtils.getStatusHeight())

    private val positionFlow by lazy {
        MutableStateFlow(
            SettingsRepository.overlayPositions.value[positionKey].let {
                if (it != null && it.size >= 2) {
                    it
                } else {
                    defaultPosition
                }
            }
        )
    }

    init {
        useStopServiceReceiver()
        onCreated {
            val shareContextLease = acquireShareContext()
            onDestroyed { shareContextLease.close() }
            aliveSize++
            onDestroyed { ThreadUtils.runMainOrPost(1000) { aliveSize-- } }
            onDestroyed { savePosition() }
            val host = resolveOverlayWindowHost()
            if (host == null) {
                onOverlayWindowHostUnavailable()
                return@onCreated
            }
            attachView(host)
            onDestroyed {
                OverlayWindowRegistry.unregister(view)
                // 截图期间窗口可能已被摘除，移除失败是正常情况。
                runCatching { host.windowManager.removeView(view) }
            }
        }
    }

    private fun savePosition() {
        val position = positionFlow.value
        SettingsRepository.updateOverlayPosition(positionKey, position.first(), position.last())
    }

    private fun attachView(host: OverlayWindowHost) {
        val marginX = minMargin
        val marginY = minMargin
        val windowManager = host.windowManager
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            host.type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            windowAnimations = android.R.style.Animation_Dialog
            gravity = Gravity.START or Gravity.TOP
            x = positionFlow.value.first()
            y = positionFlow.value.last()
        }
        var screenWidth = ScreenUtils.getWidth()
        var screenHeight = ScreenUtils.getHeight()
        var paramsXy = layoutParams.x to layoutParams.y
        var fixMoveFlag = 0
        val fixLimitXy = {
            screenWidth = ScreenUtils.getWidth()
            screenHeight = ScreenUtils.getHeight()
            val x = layoutParams.x.coerceIn(marginX, screenWidth - view.width - marginX)
            val y = layoutParams.y.coerceIn(
                marginY,
                screenHeight - view.height - marginY
            )
            if (x != layoutParams.x || y != layoutParams.y) {
                positionFlow.value = listOf(x, y)
                savePosition()
                val startX = layoutParams.x
                val startY = layoutParams.y
                fixMoveFlag++
                val tempFlag = fixMoveFlag
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = 300
                    addUpdateListener { animator ->
                        if (tempFlag == fixMoveFlag) {
                            val fraction = animator.animatedValue as Float
                            layoutParams.x = (startX + (x - startX) * fraction).toInt()
                            layoutParams.y = (startY + (y - startY) * fraction).toInt()
                            windowManager.updateViewLayout(view, layoutParams)
                        } else {
                            pause()
                        }
                    }
                    doOnEnd {
                        if (tempFlag == fixMoveFlag) {
                            fixMoveFlag = 0
                        }
                    }
                }.start()
            }
        }
        lifecycleScope.launch {
            // A view-level listener survives the window being detached for a capture and re-attached.
            view.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                launch { resizeFlow.emit(Unit) }
            }
            resizeFlow.debounce(100.milliseconds).collect { fixLimitXy() }
        }
        var downXy: Pair<Float, Float>? = null
        var longClickJob: Job? = null
        var viewClickEnabled = false
        var isDragging = false
        var longClickFired = false
        // 只有真正拖动过才需要写回位置，避免每次点击都触发一次设置文件落盘。
        var positionChanged = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        // 点击判定上限必须与长按时长一致：低于长按时长的手势都算点击。
        // 用 getTapTimeout()(100ms) 会把正常按压(100~200ms)整段丢弃，表现为“点好几次才生效”。
        val longPressMillis = ViewConfiguration.getLongPressTimeout()
        @SuppressLint("ClickableViewAccessibility")
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downXy = event.rawX to event.rawY
                    screenWidth = ScreenUtils.getWidth()
                    screenHeight = ScreenUtils.getHeight()
                    paramsXy = layoutParams.x to layoutParams.y
                    viewClickEnabled = isViewClickEnabled()
                    isDragging = false
                    longClickFired = false
                    positionChanged = false
                    longClickJob = null
                    longClickJob = lifecycleScope.launch {
                        delay(longPressMillis.toLong())
                        // 长按已触发：抬起时不应再判为点击。
                        longClickFired = true
                        longClickJob = null
                        if (downXy != null) {
                            onLongClickView()
                        }
                    }
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    downXy?.let { downEvent ->
                        val dx = (event.rawX - downEvent.first).toInt()
                        val dy = (event.rawY - downEvent.second).toInt()
                        if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                            isDragging = true
                        }
                        if (isDragging) {
                            longClickJob?.cancel()
                            longClickJob = null
                        }
                        // 位置修正动画期间不接受拖动，避免与动画争抢 layoutParams；
                        // 但"是否拖动"必须在上面先判定，否则动画期间的手势会被当成点击。
                        if (fixMoveFlag > 0) return@setOnTouchListener true
                        val x = dx + paramsXy.first
                        val y = dy + paramsXy.second
                        layoutParams.x = x.coerceIn(marginX, screenWidth - view.width - marginX)
                        layoutParams.y = y.coerceIn(
                            marginY,
                            screenHeight - view.height - marginY
                        )
                        positionFlow.value = listOf(layoutParams.x, layoutParams.y)
                        windowManager.updateViewLayout(view, layoutParams)
                        positionChanged = isDragging
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (positionChanged) savePosition()
                    // 长按已消费手势时不判点击；其余只要没拖动就算点击。
                    // 不再用时间窗过滤：长按计时已覆盖“按住太久”的情况。
                    if (viewClickEnabled && !isDragging && !longClickFired) {
                        onClickView()
                    }
                    downXy = null
                    longClickJob?.cancel()
                    longClickJob = null
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    if (positionChanged) savePosition()
                    downXy = null
                    longClickJob?.cancel()
                    longClickJob = null
                    true
                }

                else -> false
            }
        }
        windowManager.addView(view, layoutParams)
        OverlayWindowRegistry.register(windowManager, view, layoutParams)
    }

}
