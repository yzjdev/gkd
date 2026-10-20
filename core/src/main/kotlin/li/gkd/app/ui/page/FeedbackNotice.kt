package li.gkd.app.ui.page

import li.gkd.app.R

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import li.gkd.app.resources.*

suspend fun feedbackNotice(primary: Color): AnnotatedString = buildAnnotatedString {
    val highlight = SpanStyle(fontWeight = FontWeight.Bold, color = primary)
    append(getString(R.string.feedback_thanks_prefix))
    withStyle(highlight) { append(getString(R.string.feedback_scope)) }
    append("\n\n")
    append(getString(R.string.feedback_subscription_notice))
    withStyle(highlight) { append(getString(R.string.feedback_app_issue_prefix)) }
    append(getString(R.string.feedback_continue_suffix))
}
