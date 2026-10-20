package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import li.gkd.app.app.AppInfoRepository
import li.gkd.app.data.appinfo.PackageAppCatalog

@Composable
fun GkAppIcon(painter: Painter?, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val iconModifier = modifier.size(size)
    if (painter != null) {
        Image(painter = painter, contentDescription = null, modifier = iconModifier)
    } else {
        GkIcon(imageVector = GkIcons.Android, modifier = iconModifier)
    }
}

@Composable
fun GkAppIcon(
    appId: String,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    val catalog = AppInfoRepository.state.collectAsStateWithLifecycle().value.snapshot
    val icon = PackageAppCatalog.icon(catalog?.apps?.get(appId))
    GkAppIcon(
        painter = icon?.let { rememberDrawablePainter(it) },
        modifier = modifier,
        size = size,
    )
}
