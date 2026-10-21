package li.gkd.app.ui.page

import li.gkd.app.R

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** A host owns navigation, keyboard dismissal and persistence; the page owns its draft. */
data class EditorSession(
    val title: String,
    val hasChanges: suspend () -> Boolean,
    val saveEnabled: Boolean = true,
    val onSave: suspend () -> Unit,
    val titleContent: @Composable () -> Unit = { Text(title) },
    /** Replaces the default close button, e.g. a back arrow when the page has inner levels. */
    val navigationIcon: (@Composable () -> Unit)? = null,
    /** Extra top bar actions rendered before the save button. */
    val menuActions: (@Composable RowScope.() -> Unit)? = null,
    /**
     * Handles a back request for the page's own inner levels; return true when it consumed
     * the request (stepped back one level), false to let the host close the page.
     */
    val onBack: () -> Boolean = { false },
)
