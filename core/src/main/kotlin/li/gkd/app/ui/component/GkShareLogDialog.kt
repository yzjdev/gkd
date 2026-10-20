package li.gkd.app.ui.component

import li.gkd.app.R

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import li.gkd.app.resources.*
import androidx.compose.ui.res.stringResource

@Composable
fun GkShareLogDialog(
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    onUpload: () -> Unit
) {
    GkDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            val modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
            Text(
                text = stringResource(R.string.action_share),
                modifier = Modifier
                    .clickable(onClick = onShare)
                    .then(modifier),
            )
            Text(
                text = stringResource(R.string.action_save_to_downloads),
                modifier = Modifier
                    .clickable(onClick = onSave)
                    .then(modifier),
            )
            Text(
                text = stringResource(R.string.upload_generate_link),
                modifier = Modifier
                    .clickable(onClick = onUpload)
                    .then(modifier),
            )
        }
    }
}
