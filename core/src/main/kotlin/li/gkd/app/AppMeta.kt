package li.gkd.app

import android.content.pm.ApplicationInfo
import kotlinx.serialization.Serializable
import li.gkd.app.data.appinfo.PackageAppCatalog.selfAppInfo
import li.gkd.app.network.AppLinks

@Serializable
data class AppMeta(
    val channel: String,
    val buildKey: String,
    val commitId: String,
    val commitTime: Long,
    val tagName: String?,
    val debuggable: Boolean,
    val versionCode: Int,
    val versionName: String,
    val appId: String,
    val appName: String,
) {
    val commitUrl = "${AppLinks.Repository}/".run {
        plus(if (tagName != null) "tree/$tagName" else "commit/$commitId")
    }
    val isGkdChannel get() = channel == "gkd"
    val updateEnabled get() = isGkdChannel
    val isBeta get() = versionName.contains("beta")
}

val META: AppMeta by lazy {
    AppMeta(
        channel = app.getMetaString("channel"),
        buildKey = app.getMetaString("buildKey"),
        commitId = app.getMetaString("commitId"),
        commitTime = app.getMetaString("commitTime").toLong(),
        tagName = app.getMetaString("tagName").takeIf { it.isNotEmpty() },
        debuggable = app.applicationInfoWithMetadata.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0,
        versionCode = selfAppInfo.versionCode,
        versionName = selfAppInfo.versionName!!,
        appId = app.packageName!!,
        appName = app.applicationInfoWithMetadata.loadLabel(app.packageManager).toString(),
    )
}
