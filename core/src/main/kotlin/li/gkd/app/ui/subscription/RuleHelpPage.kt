package li.gkd.app.ui.subscription

import li.gkd.app.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import li.gkd.app.resources.*
import li.gkd.app.ui.MainViewModel
import li.gkd.app.ui.component.GkIconButton
import li.gkd.app.ui.component.GkIcons
import li.gkd.app.ui.component.GkPageBottomSpace
import li.gkd.app.ui.component.GkScaffold
import li.gkd.app.ui.component.GkTopAppBar
import li.gkd.app.ui.component.gkTopAppBarScroll
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource

/** 表格里的一行：属性名、类型（动作取值表没有）、说明。 */
private data class RuleHelpEntry(
    val name: String,
    val type: Int?,
    val description: Int,
)

/** schema 的字段直接转成表格行，类型来自控件类型。 */
private fun List<VisualField>.asEntries(): List<RuleHelpEntry> = map { field ->
    RuleHelpEntry(field.name, field.control.typeResource, field.description)
}

// 每张表对应一个 data class，字段顺序与其声明一致。
private val appGroupEntries = appGroupFields.asEntries()
private val globalGroupEntries = globalGroupFields.asEntries()
private val appRuleEntries = appRuleFields.asEntries()
private val globalRuleEntries = globalRuleFields.asEntries()

/** 动作取值表只有取值与说明，没有类型列。 */
private val actionEntries = RuleVisualAction.entries.map { action ->
    RuleHelpEntry(action.value, null, action.descriptionResource)
}

@Composable
fun RuleHelpPage() {
    val mainVm = MainViewModel.requireCurrent()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    GkScaffold(
        modifier = Modifier.gkTopAppBarScroll(scrollBehavior),
        topBar = {
            GkTopAppBar(
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    GkIconButton(imageVector = GkIcons.ArrowBack, onClick = mainVm.navigator::pop)
                },
                title = { Text(stringResource(R.string.rule_help)) },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(contentPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.rule_help_note),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RuleHelpTable(R.string.rule_help_app_props, appGroupEntries)
            RuleHelpTable(R.string.rule_help_global_props, globalGroupEntries)
            RuleHelpTable(R.string.rule_help_app_rule_props, appRuleEntries)
            RuleHelpTable(R.string.rule_help_global_rule_props, globalRuleEntries)
            RuleHelpTable(R.string.rule_help_action_values, actionEntries)
            GkPageBottomSpace()
        }
    }
}

private const val PROPERTY_WEIGHT = 0.3f
private const val WEIGHTED_TYPE_WEIGHT = 0.24f
private const val WEIGHTED_DESCRIPTION_WEIGHT = 0.46f
private const val TYPELESS_DESCRIPTION_WEIGHT = 0.7f

@Composable
private fun RuleHelpTable(title: Int, entries: List<RuleHelpEntry>) {
    // 动作取值表格只列取值与说明，其余表格额外给出该属性应填的类型。
    val showType = entries.any { it.type != null }
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(
            text = stringResource(title),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        RuleHelpRow(
            name = stringResource(R.string.rule_help_property),
            type = if (showType) stringResource(R.string.rule_help_type) else null,
            description = stringResource(R.string.rule_help_description),
            showType = showType,
            header = true,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
        entries.forEach { entry ->
            RuleHelpRow(
                name = entry.name,
                type = entry.type?.let { stringResource(it) },
                description = stringResource(entry.description),
                showType = showType,
                header = false,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest)
        }
    }
}

@Composable
private fun RuleHelpRow(
    name: String,
    type: String?,
    description: String,
    showType: Boolean,
    header: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
            .padding(horizontal = 20.dp, vertical = if (header) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val nameModifier = Modifier.weight(PROPERTY_WEIGHT)
        if (header) {
            Text(
                text = name,
                modifier = nameModifier,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = name,
                modifier = nameModifier,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        if (showType) {
            Text(
                text = type.orEmpty(),
                modifier = Modifier.weight(WEIGHTED_TYPE_WEIGHT),
                style = if (header) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = description,
            modifier = Modifier.weight(
                if (showType) WEIGHTED_DESCRIPTION_WEIGHT else TYPELESS_DESCRIPTION_WEIGHT
            ),
            style = if (header) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
