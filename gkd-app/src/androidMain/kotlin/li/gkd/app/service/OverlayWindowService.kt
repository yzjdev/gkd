package li.gkd.app.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import li.gkd.app.app
import li.gkd.app.matching.input.AndroidForegroundState
import li.gkd.app.permission.PermissionStates
import li.gkd.app.resources.*
import li.gkd.app.settings.AppLanguage
import li.gkd.app.settings.SettingsRepository
import li.gkd.app.ui.component.GkAndroidLanguageEnvironment
import li.gkd.app.ui.component.GkIcon
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.icon.DragPan
import li.gkd.app.ui.style.AppTheme
import li.gkd.app.ui.style.iconTextSize
import li.gkd.app.util.BarUtils
import li.gkd.app.util.ScreenUtils
import li.gkd.app.util.ThreadUtils
import li.gkd.app.util.ToastUtils
import li.gkd.app.util.px
import org.jetbrains.compose.resources.stringResource

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
    var overlayContentHidden by mutableStateOf(false)
    val scope = MainScope()

    init {
        scope.launch {
            var canDrawOverlays = PermissionStates.drawOverlays.updateAndGet()
            AndroidForegroundState.observedForegroundFlow.collectLatest {
                repeat(6) { attempt ->
                    if (attempt > 0) delay(500.milliseconds)
                    val oldV = canDrawOverlays
                    val newV = PermissionStates.drawOverlays.updateAndGet()
                    canDrawOverlays = newV
                    if (!newV && oldV) {
                        ToastUtils.show(Res.string.overlay_screen_denied.getSync())
                        return@collectLatest
                    }
                }
            }
        }
    }
}

abstract class OverlayWindowService(
    private val positionKey: String,
    private val accessibilityOverlay: Boolean = false,
) : LifecycleHookService(), SavedStateRegistryOwner {

    companion object {
        private var aliveSize = 0
        val isAnyAlive: Boolean
            get() = aliveSize > 0
    }

    // TYPE_ACCESSIBILITY_OVERLAY 只能由无障碍服务的 WindowManager 添加；
    // 无障碍服务未连接时退回应用悬浮窗，避免悬浮窗直接缺失
    private val accessibilityWindowManager =
        if (accessibilityOverlay) A11yService.instance?.wm else null

    private val windowManager = accessibilityWindowManager ?: app.windowManager

    private val windowType =
        if (accessibilityWindowManager != null) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }

    private val resizeFlow = MutableSharedFlow<Unit>()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lifecycleScope.launch { resizeFlow.emit(Unit) }
    }

    private val registryController =
        SavedStateRegistryController.create(this).apply {
            performAttach()
            performRestore(null)
        }
    override val savedStateRegistry
        get() = registryController.savedStateRegistry

    @Composable abstract fun ComposeContent()

    @Composable
    fun ClosableTitle(
        title: String,
        onMinimizeRequest: (() -> Unit)? = null,
        minimizeContentDescription: String = stringResource(Res.string.window_minimize),
        label: String? = null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            GkIcon(imageVector = DragPan, modifier = Modifier.iconTextSize())
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, modifier = Modifier.weight(1f, fill = false))
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier =
                            Modifier.padding(horizontal = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.shapes.extraSmall,
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
            if (onMinimizeRequest != null) {
                GkIcon(
                    imageVector = GkIcons.CollapseContent,
                    contentDescription = minimizeContentDescription,
                    modifier =
                        Modifier.clip(MaterialTheme.shapes.extraSmall)
                            .clickable(onClick = onMinimizeRequest)
                            .iconTextSize(),
                )
            }
            GkIcon(
                imageVector = GkIcons.Close,
                modifier =
                    Modifier.clip(MaterialTheme.shapes.extraSmall)
                        .clickable(
                            onClick = {
                                stopSelf()
                            },
                        )
                        .iconTextSize(),
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
                val locale by AppLanguage.locale.collectAsStateWithLifecycle()
                GkAndroidLanguageEnvironment(locale) {
                    AppTheme(invertedTheme = true) {
                        Box(
                            modifier =
                                Modifier.drawWithContent {
                                    if (!shareContext.overlayContentHidden) drawContent()
                                },
                        ) {
                            ComposeContent()
                        }
                    }
                }
            }
        }
    }

    protected val isOverlayContentHidden
        get() = shareContext.overlayContentHidden

    protected suspend fun <T> withAllOverlaysHidden(block: suspend () -> T): T {
        shareContext.overlayContentHidden = true
        setWindowVisible(false)
        return try {
            awaitOverlayContentHidden()
            block()
        } finally {
            showOverlayContent()
        }
    }

    protected fun showOverlayContent() {
        shareContext.overlayContentHidden = false
        setWindowVisible(true)
    }

    // 只跳过 Compose 绘制会把上一帧内容留在窗口 Surface 上，截图仍会带上悬浮窗；
    // 这里同时把视图和窗口 alpha 置零，让 Surface 重绘为透明
    private fun setWindowVisible(visible: Boolean) {
        val alpha = if (visible) 1f else 0f
        view.alpha = alpha
        val params = attachedLayoutParams ?: return
        params.alpha = alpha
        windowManager.updateViewLayout(view, params)
    }

    private suspend fun awaitOverlayContentHidden() =
        suspendCancellableCoroutine { continuation ->
            val step =
                object : Runnable {
                    private var remaining = 2

                    override fun run() {
                        if (!continuation.isActive) return
                        remaining--
                        if (remaining > 0) {
                            view.postOnAnimation(this)
                        } else {
                            continuation.resume(Unit)
                        }
                    }
                }
            continuation.invokeOnCancellation { view.removeCallbacks(step) }
            view.postOnAnimation(step)
        }

    private val minMargin
        get() = 10.dp.px.toInt()

    private val defaultPosition
        get() = listOf(minMargin, BarUtils.getStatusHeight())

    private lateinit var shareContext: ShareContext

    private var attachedLayoutParams: WindowManager.LayoutParams? = null

    private val positionFlow by lazy {
        MutableStateFlow(
            SettingsRepository.overlayPositions.value[positionKey].let {
                if (it != null && it.size >= 2) {
                    it
                } else {
                    defaultPosition
                }
            },
        )
    }

    init {
        useStopServiceReceiver()
        onCreated {
            val shareContextLease = acquireShareContext()
            shareContext = shareContextLease.context
            onDestroyed { shareContextLease.close() }
            aliveSize++
            onDestroyed { ThreadUtils.runMainOrPost(1000) { aliveSize-- } }
            onDestroyed { savePosition() }
            attachView()
            onDestroyed { windowManager.removeView(view) }
        }
    }

    private fun savePosition() {
        val position = positionFlow.value
        SettingsRepository.updateOverlayPosition(positionKey, position.first(), position.last())
    }

    private fun attachView() {
        val marginX = minMargin
        val marginY = minMargin
        val layoutParams =
            WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    windowType,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                )
                .apply {
                    windowAnimations = android.R.style.Animation_Dialog
                    gravity = Gravity.START or Gravity.TOP
                    x = positionFlow.value.first()
                    y = positionFlow.value.last()
                }
        var screenSize = ScreenUtils.getSize()
        var paramsXy = layoutParams.x to layoutParams.y
        var positionAnimator: ValueAnimator? = null
        onDestroyed { positionAnimator?.cancel() }
        val fixLimitXy = {
            screenSize = ScreenUtils.getSize()
            val x = layoutParams.x.coerceIn(marginX, screenSize.width - view.width - marginX)
            val y =
                layoutParams.y.coerceIn(
                    marginY,
                    screenSize.height - view.height - marginY,
                )
            if (x != layoutParams.x || y != layoutParams.y) {
                positionFlow.value = listOf(x, y)
                savePosition()
                val startX = layoutParams.x
                val startY = layoutParams.y
                positionAnimator?.cancel()
                val animator =
                    ValueAnimator.ofFloat(0f, 1f).apply {
                        duration = 300
                        addUpdateListener { animator ->
                            val fraction = animator.animatedValue as Float
                            layoutParams.x = (startX + (x - startX) * fraction).toInt()
                            layoutParams.y = (startY + (y - startY) * fraction).toInt()
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                        doOnEnd {
                            positionAnimator = null
                        }
                    }
                positionAnimator = animator
                animator.start()
            }
        }
        lifecycleScope.launch {
            view.viewTreeObserver.addOnGlobalLayoutListener { launch { resizeFlow.emit(Unit) } }
            resizeFlow.debounce(100.milliseconds).collect { fixLimitXy() }
        }
        var downXy: Pair<Float, Float>? = null
        var viewClickEnabled = false
        var isDragging = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        val gestureDetector =
            GestureDetector(
                this,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean {
                        positionAnimator?.cancel()
                        downXy = e.rawX to e.rawY
                        screenSize = ScreenUtils.getSize()
                        paramsXy = layoutParams.x to layoutParams.y
                        viewClickEnabled = isViewClickEnabled()
                        isDragging = false
                        return true
                    }

                    override fun onSingleTapUp(e: MotionEvent): Boolean {
                        if (viewClickEnabled && !isDragging) {
                            onClickView()
                        }
                        return true
                    }

                    override fun onLongPress(e: MotionEvent) {
                        if (!isDragging) {
                            onLongClickView()
                        }
                    }
                },
            )
        @SuppressLint("ClickableViewAccessibility")
        view.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    downXy?.let { downEvent ->
                        val dx = (event.rawX - downEvent.first).toInt()
                        val dy = (event.rawY - downEvent.second).toInt()
                        if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                            isDragging = true
                        }
                        if (isDragging) {
                            val x = dx + paramsXy.first
                            val y = dy + paramsXy.second
                            layoutParams.x =
                                x.coerceIn(marginX, screenSize.width - view.width - marginX)
                            layoutParams.y =
                                y.coerceIn(
                                    marginY,
                                    screenSize.height - view.height - marginY,
                                )
                            positionFlow.value = listOf(layoutParams.x, layoutParams.y)
                            windowManager.updateViewLayout(view, layoutParams)
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    savePosition()
                    downXy = null
                    true
                }

                else -> true
            }
        }
        attachedLayoutParams = layoutParams
        windowManager.addView(view, layoutParams)
    }
}
