package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll

@Composable
fun Modifier.gkTopAppBarScroll(behavior: TopAppBarScrollBehavior): Modifier =
    nestedScroll(behavior.nestedScrollConnection)
