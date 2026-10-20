package li.gkd.app.ui.theme

import li.gkd.app.R

data class AppearanceState(val darkTheme: Boolean?, val dynamicColor: Boolean) {
    fun isDark(systemDark: Boolean, inverted: Boolean = false): Boolean =
        (darkTheme ?: systemDark) xor inverted
}

