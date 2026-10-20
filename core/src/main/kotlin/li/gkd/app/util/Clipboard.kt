package li.gkd.app.util

import li.gkd.app.R

import li.gkd.app.platform.writeClipboardText
import li.gkd.app.resources.*

fun copyText(text: String) {
    writeClipboardText(text)
    ToastUtils.show(R.string.copy_success.getSync())
}
