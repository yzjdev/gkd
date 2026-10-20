package li.gkd.app.util

import li.gkd.app.R

import com.hjq.toast.Toaster
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import li.gkd.app.app.applicationScope
import li.gkd.app.platform.lifecycle.MainActivityVisibility
import li.gkd.app.service.OverlayWindowService
import li.songe.codeorigin.CallSite

object ToastUtils {
    fun show(
        text: CharSequence,
        forced: Boolean = false,
        delayMillis: Long = 0L,
        @CallSite loc: String = "",
    ) {
        applicationScope().launch(Dispatchers.Main.immediate) {
            if (delayMillis > 0) delay(delayMillis)
            showPlatformToast(text, forced)
            if (loc.isNotEmpty()) LogUtils.d(text, loc = loc)
        }
    }
}

fun showPlatformToast(text: CharSequence, forced: Boolean) {
    if (forced || MainActivityVisibility.isVisible || OverlayWindowService.isAnyAlive) {
        Toaster.show(text)
    }
}
