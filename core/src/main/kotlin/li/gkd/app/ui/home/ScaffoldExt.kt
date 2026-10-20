package li.gkd.app.ui.home

import li.gkd.app.R

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import li.gkd.app.ui.component.GkTopAppBar

data class ScaffoldExt(
    val navItem: BottomNavItem,
    val modifier: Modifier = Modifier,
    val topBar: @Composable () -> Unit = {
        GkTopAppBar(title = {
            Text(
                text = navItem.label,
            )
        })
    },
    val floatingActionButton: @Composable () -> Unit = {},
    val snackbarHost: @Composable () -> Unit = {},
    val content: @Composable (PaddingValues) -> Unit
)

