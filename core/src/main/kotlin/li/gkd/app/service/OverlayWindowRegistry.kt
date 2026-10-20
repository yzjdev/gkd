package li.gkd.app.service

import li.gkd.app.R

import android.view.Choreographer
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import li.gkd.app.util.LogUtils
import kotlin.coroutines.resume

/** 摘屏前等待的帧数：留给 SurfaceFlinger 把悬浮窗从合成层移除。 */
private const val CAPTURE_SETTLE_FRAMES = 5

/** 帧等待之后的兜底延时：部分 ROM 的合成延迟更长。 */
private const val CAPTURE_SETTLE_MILLIS = 300L

/**
 * 悬浮窗是独立的系统窗口，整屏截图必然把它拍进去。
 * 无法做窗口级截图时，只能把窗口从 WindowManager 摘掉、等合成层真正退场后再截图。
 */
object OverlayWindowRegistry {
    private class Target(
        val windowManager: WindowManager,
        val view: View,
        val params: WindowManager.LayoutParams,
        var attached: Boolean = true,
        var retired: Boolean = false,
    )

    private val targets = mutableListOf<Target>()

    fun register(windowManager: WindowManager, view: View, params: WindowManager.LayoutParams) {
        if (targets.none { it.view === view }) {
            targets += Target(windowManager, view, params)
        }
    }

    /** 所属服务已销毁：正在进行的截图结束后不得再把窗口挂回去。 */
    fun unregister(view: View) {
        targets.removeAll { target ->
            (target.view === view).also { if (it) target.retired = true }
        }
    }

    /** 摘掉所有悬浮窗，等合成退场后执行 [block]，结束后原样挂回。 */
    suspend fun <T> withOverlaysHidden(block: suspend () -> T): T {
        val detached = withContext(Dispatchers.Main.immediate) {
            val removed = targets.filter { it.attached }.onEach { detach(it) }
            if (removed.isNotEmpty()) {
                // removeView 返回只代表请求已提交，SurfaceFlinger 仍需几帧才不再合成它；
                // 隐藏后立即截图会拍到带悬浮窗的旧帧，所以等帧之后再补一小段延时。
                repeat(CAPTURE_SETTLE_FRAMES) { awaitFrame() }
                delay(CAPTURE_SETTLE_MILLIS)
            }
            removed
        }
        if (detached.isEmpty()) return block()
        return try {
            block()
        } finally {
            withContext(Dispatchers.Main.immediate) { detached.forEach { attach(it) } }
        }
    }

    private suspend fun awaitFrame() = suspendCancellableCoroutine { continuation ->
        val callback = Choreographer.FrameCallback {
            if (continuation.isActive) continuation.resume(Unit)
        }
        continuation.invokeOnCancellation { Choreographer.getInstance().removeFrameCallback(callback) }
        Choreographer.getInstance().postFrameCallback(callback)
    }

    private fun detach(target: Target) {
        if (!target.attached) return
        target.attached = false
        // 先让视图停止绘制，再移除窗口：仅移除窗口时系统可能仍保留最后一次合成的表面。
        runCatching { target.view.visibility = View.INVISIBLE }
        runCatching { target.windowManager.removeViewImmediate(target.view) }
            .onFailure { LogUtils.d(it) }
    }

    private fun attach(target: Target) {
        if (target.attached || target.retired) return
        target.attached = true
        runCatching { target.view.visibility = View.VISIBLE }
        runCatching { target.windowManager.addView(target.view, target.params) }
            .onFailure {
                LogUtils.d(it)
                target.attached = false
            }
    }
}
