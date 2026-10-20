package li.gkd.app.platform

import li.gkd.app.R

import android.content.ClipData
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import li.gkd.app.app
import java.io.File

/** Requested 表示已唤起系统安装器，不代表安装已完成。 */
fun requestPackageInstall(file: File): PlatformResult<Unit> {
    val uri = FileProvider.getUriForFile(app, "${app.packageName}.provider", file)
    app.startActivity(Intent(Intent.ACTION_VIEW).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setDataAndType(uri, "application/vnd.android.package-archive")
    })
    return PlatformResult.Success(Unit)
}

/** Success 表示打开请求已交给系统。 */
fun openExternalUri(uri: String): PlatformResult<Unit> {
    app.startActivity(
        Intent(Intent.ACTION_VIEW, uri.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    return PlatformResult.Success(Unit)
}

fun writeClipboardText(text: String) {
    app.clipboardManager
        .setPrimaryClip(ClipData.newPlainText(app.packageName, text))
}
