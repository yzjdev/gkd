package li.gkd.app.ui.style

import androidx.collection.LruCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.qiplat.sweetline.HighlightConfig
import com.qiplat.sweetline.HighlightEngine
import com.qiplat.sweetline.TextAnalyzer
import com.qiplat.sweetline.util.AssetUtils
import li.gkd.app.app
import li.gkd.app.logging.LogUtils

private const val Json5SyntaxAsset = "syntaxes/json5.json"
private const val Json5AnalyzerFileName = "rule.json5"

/** SweetLine 引擎随进程存活；初始化失败时退回纯文本，编辑器仍可正常输入。 */
private object SweetLineJson5 {
    private val styleIds =
        mapOf(
            "comment" to 1,
            "keyword" to 2,
            "string" to 3,
            "number" to 4,
            "punctuation" to 5,
            "builtin" to 6,
            "url" to 7,
        )

    val engine: HighlightEngine? by lazy {
        runCatching {
                HighlightEngine(HighlightConfig(true, false, 2)).apply {
                    styleIds.forEach { (name, id) -> registerStyleName(name, id) }
                    compileSyntaxFromJson(AssetUtils.readAsset(app, Json5SyntaxAsset))
                }
            }
            .onSuccess { LogUtils.i("SweetLine 规则高亮已启用") }
            .getOrElse {
                LogUtils.e("SweetLine 初始化失败，回退内置 JSON5 高亮", it)
                null
            }
    }

    val analyzer: TextAnalyzer? by lazy { engine?.createAnalyzerByFileName(Json5AnalyzerFileName) }
}

/** SweetLine 高亮自己的 token 配色，与内置 JSON5 高亮区分，便于确认引擎确实生效。 */
private val darkStyleColors =
    mapOf(
        1 to Color(0xFF676E95),
        2 to Color(0xFFC792EA),
        3 to Color(0xFFC3E88D),
        4 to Color(0xFFF78C6C),
        5 to Color(0xFF89DDFF),
        6 to Color(0xFFFFCB6B),
        7 to Color(0xFF82AAFF),
    )

private val lightStyleColors =
    mapOf(
        1 to Color(0xFF6A737D),
        2 to Color(0xFFA626A4),
        3 to Color(0xFF50A14F),
        4 to Color(0xFF986801),
        5 to Color(0xFF0184BC),
        6 to Color(0xFFC18401),
        7 to Color(0xFF4078F2),
    )

private class SweetLineJson5Transformation(val dark: Boolean) : VisualTransformation {
    private val cache = LruCache<String, TransformedText>(0xF)
    private val styleCache = HashMap<Int, SpanStyle>()
    private val styleColors = if (dark) darkStyleColors else lightStyleColors

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        if (source.isBlank() || source.length > JSON5_LARGE_TEXT_THRESHOLD) {
            return VisualTransformation.None.filter(text)
        }
        cache[source]?.let {
            return it
        }
        val annotated =
            runCatching { highlight(source) }
                .getOrElse {
                    LogUtils.e("SweetLine 高亮失败，回退内置 JSON5 高亮", it)
                    return fallback(text)
                }
        return TransformedText(text = annotated, offsetMapping = OffsetMapping.Identity).apply {
            cache.put(source, this)
        }
    }

    private fun fallback(text: AnnotatedString): TransformedText =
        getJson5Transformation(dark).filter(text)

    private fun highlight(source: String): AnnotatedString {
        val analyzer = SweetLineJson5.analyzer ?: return AnnotatedString(source)
        val document = analyzer.analyzeText(source)
        return buildAnnotatedString {
            append(source)
            for (line in document.lines) {
                for (span in line.spans) {
                    val style = styleOf(span.styleId) ?: continue
                    val start = span.range.start.index
                    val end = span.range.end.index
                    if (start >= 0 && start < end && end <= source.length) {
                        addStyle(style, start, end)
                    }
                }
            }
        }
    }

    private fun styleOf(styleId: Int): SpanStyle? {
        val color = styleColors[styleId] ?: return null
        return styleCache.getOrPut(styleId) { SpanStyle(color = color) }
    }
}

private val darkSweetLineTransformation by lazy { SweetLineJson5Transformation(true) }
private val lightSweetLineTransformation by lazy { SweetLineJson5Transformation(false) }

/** Android 端的规则文本高亮，替换默认的 JSON5 高亮实现。 */
fun sweetLineRuleTextTransformation(dark: Boolean): VisualTransformation =
    if (dark) darkSweetLineTransformation else lightSweetLineTransformation
