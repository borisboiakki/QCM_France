package com.example.qcmfrance.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp

/**
 * Rendu markdown **minimal** en Compose natif (pas de WebView, pas de dépendance externe).
 *
 * Sous-ensemble supporté, suffisant pour les fiches officielles générées par `fetch_fiches.py` :
 *  - titres `#`, `##`, `###`
 *  - paragraphes (séparés par une ligne vide)
 *  - listes à puces (`- ` ou `* `)
 *  - inline : `**gras**`, `*italique*`, liens `[texte](url)` (ouverts via l'annotation de lien).
 *
 * Tout le reste est rendu comme texte brut. Les liens utilisent la couleur `primary` du thème.
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val blocks = rememberMarkdownBlocks(markdown)
    val linkColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    text = parseInline(block.text, linkColor),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineSmall
                        2 -> MaterialTheme.typography.titleLarge
                        else -> MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )

                is MdBlock.Paragraph -> Text(
                    text = parseInline(block.text, linkColor),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                is MdBlock.BulletItem -> Row(
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "•  ",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = parseInline(block.text, linkColor),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class BulletItem(val text: String) : MdBlock
}

/** Découpe le markdown en blocs. Léger : recalcule seulement si le texte change. */
@Composable
private fun rememberMarkdownBlocks(markdown: String): List<MdBlock> =
    androidx.compose.runtime.remember(markdown) { parseBlocks(markdown) }

private fun parseBlocks(markdown: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val paragraph = StringBuilder()

    fun flushParagraph() {
        if (paragraph.isNotBlank()) {
            blocks += MdBlock.Paragraph(paragraph.trim().toString())
        }
        paragraph.setLength(0)
    }

    markdown.replace("\r\n", "\n").split("\n").forEach { rawLine ->
        val line = rawLine.trimEnd()
        when {
            line.isBlank() -> flushParagraph()

            line.startsWith("#") -> {
                flushParagraph()
                val level = line.takeWhile { it == '#' }.length.coerceIn(1, 6)
                blocks += MdBlock.Heading(level, line.drop(level).trim())
            }

            line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                flushParagraph()
                blocks += MdBlock.BulletItem(line.trimStart().drop(2).trim())
            }

            else -> {
                // accumule les lignes contiguës en un paragraphe (espace de jointure)
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(line.trim())
            }
        }
    }
    flushParagraph()
    return blocks
}

/**
 * Parse l'inline markdown (`**gras**`, `*italique*`, `[texte](url)`) en [AnnotatedString].
 * Un simple automate à balayage — pas de récursion imbriquée complexe.
 */
private fun parseInline(text: String, linkColor: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        var i = 0
        val n = text.length
        val linkStyle = TextLinkStyles(
            style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
        )
        while (i < n) {
            val c = text[i]
            when {
                // lien [texte](url)
                c == '[' -> {
                    val closeLabel = text.indexOf(']', i + 1)
                    val openUrl = if (closeLabel != -1) closeLabel + 1 else -1
                    if (closeLabel != -1 && openUrl < n && text[openUrl] == '(') {
                        val closeUrl = text.indexOf(')', openUrl + 1)
                        if (closeUrl != -1) {
                            val label = text.substring(i + 1, closeLabel)
                            val url = text.substring(openUrl + 1, closeUrl)
                            withLink(LinkAnnotation.Url(url, linkStyle)) {
                                appendInlineEmphasis(label)
                            }
                            i = closeUrl + 1
                            continue
                        }
                    }
                    append(c); i++
                }

                // gras **texte**
                c == '*' && i + 1 < n && text[i + 1] == '*' -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        appendInlineEmphasis(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else { append(c); i++ }
                }

                // italique *texte*
                c == '*' -> {
                    val end = text.indexOf('*', i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else { append(c); i++ }
                }

                else -> { append(c); i++ }
            }
        }
    }

/** Applique le gras/italique à l'intérieur d'un libellé de lien (une passe simple). */
private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlineEmphasis(text: String) {
    var i = 0
    val n = text.length
    while (i < n) {
        val c = text[i]
        when {
            c == '*' && i + 1 < n && text[i + 1] == '*' -> {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(text.substring(i + 2, end)); pop(); i = end + 2
                } else { append(c); i++ }
            }
            else -> { append(c); i++ }
        }
    }
}
