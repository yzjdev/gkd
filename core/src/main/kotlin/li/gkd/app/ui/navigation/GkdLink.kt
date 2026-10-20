package li.gkd.app.ui.navigation

import li.gkd.app.R

import li.gkd.app.ui.home.BottomNavItem
import java.net.URI
import java.net.URLDecoder

sealed interface GkdLink {
    data class Home(val tab: Int?) : GkdLink
    data class Page(val route: AppRoute) : GkdLink
    data object WeChatScanner : GkdLink

    companion object {
        fun parse(value: String): GkdLink? = runCatching {
            val uri = URI(value)
            if (!uri.scheme.equals("gkd", ignoreCase = true)) null
            else when (uri.host) {
                "page" -> when (uri.path.orEmpty()) {
                    "" -> Home(
                        uri.rawQuery?.split('&')?.firstOrNull { it.substringBefore('=') == "tab" }
                            ?.substringAfter('=', "")
                            ?.let { URLDecoder.decode(it, "UTF-8").toIntOrNull() })

                    "/1" -> Page(AdvancedPageRoute)
                    // 快照页已并入主界面标签，旧链接继续指向该标签。
                    "/2" -> Home(BottomNavItem.Snapshot.key)
                    "/3", "/4" -> Page(PrivilegeServiceRoute)
                    else -> null
                }

                "invoke" -> if (uri.path == "/1") WeChatScanner else null
                else -> null
            }
        }.getOrNull()
    }
}
