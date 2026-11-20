package li.gkd.app.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import li.gkd.app.ai.AiRuleResult
import li.gkd.app.resources.*
import li.gkd.app.ui.component.GkAlertDialog
import li.gkd.app.ui.component.GkCopyableText
import li.gkd.app.util.copyText
import org.jetbrains.compose.resources.stringResource

/**
 * Shows a generated rule group and offers to append it to the local subscription of the snapshot's
 * app. Only a validated group can be added; a rejected answer can only be copied or closed.
 */
@Composable
fun GkAiRuleResultDialog(
    result: AiRuleResult,
    onAdd: () -> Unit,
    onDismiss: () -> Unit,
) {
    val success = result as? AiRuleResult.Success
    val text =
        when (result) {
            is AiRuleResult.Success -> result.text
            is AiRuleResult.Invalid ->
                stringResource(Res.string.ai_generate_rule_failed) +
                    if (result.detail.isBlank()) "" else "\n\n" + result.detail
        }
    GkAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = {
            Text(
                stringResource(
                    if (success != null) Res.string.ai_generate_rule
                    else Res.string.ai_generate_rule_failed
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    GkCopyableText(
                        text = AnnotatedString(text),
                        onCopy = ::copyText,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(12.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            if (success != null) {
                TextButton(onClick = onAdd) {
                    Text(stringResource(Res.string.ai_rule_add))
                }
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_close))
            }
        },
    )
}
