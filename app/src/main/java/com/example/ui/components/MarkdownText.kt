package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownFormattedText(
    text: String,
    isUser: Boolean = false,
    modifier: Modifier = Modifier
) {
    val baseTextColor = if (isUser) Color.White else Color(0xFF1E293B)
    val headerColor = if (isUser) Color.White else Color(0xFF0F766E)
    val bulletColor = if (isUser) Color(0xFFE0F2FE) else Color(0xFF0F766E)
    val codeBgColor = if (isUser) Color(0x33FFFFFF) else Color(0xFFF1F5F9)
    val codeTextColor = if (isUser) Color.White else Color(0xFF0F766E)

    val lines = text.split("\n")

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var i = 0
        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            when {
                // Empty line
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Horizontal Rule
                trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(
                        color = if (isUser) Color(0x40FFFFFF) else Color(0xFFE2E8F0),
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Header Level 1 / 2 (## or #)
                trimmed.startsWith("## ") || trimmed.startsWith("# ") -> {
                    val headerText = trimmed.removePrefix("## ").removePrefix("# ").trim()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parseInlineMarkdown(headerText, headerColor, codeBgColor, codeTextColor),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            lineHeight = 21.sp
                        ),
                        color = headerColor
                    )
                }

                // Header Level 3 (### )
                trimmed.startsWith("### ") -> {
                    val headerText = trimmed.removePrefix("### ").trim()
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = parseInlineMarkdown(headerText, headerColor, codeBgColor, codeTextColor),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        color = headerColor
                    )
                }

                // Header Level 4 (#### )
                trimmed.startsWith("#### ") -> {
                    val headerText = trimmed.removePrefix("#### ").trim()
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = parseInlineMarkdown(headerText, headerColor, codeBgColor, codeTextColor),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = headerColor
                    )
                }

                // Bullet item (•, -, *)
                trimmed.startsWith("• ") || trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val itemText = when {
                        trimmed.startsWith("• ") -> trimmed.removePrefix("• ")
                        trimmed.startsWith("- ") -> trimmed.removePrefix("- ")
                        else -> trimmed.removePrefix("* ")
                    }.trim()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            ),
                            color = bulletColor,
                            modifier = Modifier.padding(end = 6.dp, top = 1.dp)
                        )
                        Text(
                            text = parseInlineMarkdown(itemText, baseTextColor, codeBgColor, codeTextColor),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = baseTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Numbered list (e.g. 1., 2., 3.)
                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val match = Regex("""^(\d+)\.\s+(.*)""").find(trimmed)
                    val num = match?.groupValues?.getOrNull(1) ?: "1"
                    val itemText = match?.groupValues?.getOrNull(2) ?: trimmed

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isUser) Color(0x33FFFFFF) else Color(0xFFDCFCE7),
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = num,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUser) Color.White else Color(0xFF166534)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = parseInlineMarkdown(itemText, baseTextColor, codeBgColor, codeTextColor),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            ),
                            color = baseTextColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Blockquote (> )
                trimmed.startsWith("> ") -> {
                    val quoteText = trimmed.removePrefix("> ").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                color = if (isUser) Color(0x22FFFFFF) else Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isUser) Color(0x44FFFFFF) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = parseInlineMarkdown(quoteText, baseTextColor, codeBgColor, codeTextColor),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            ),
                            color = baseTextColor
                        )
                    }
                }

                // Normal paragraph text
                else -> {
                    Text(
                        text = parseInlineMarkdown(trimmed, baseTextColor, codeBgColor, codeTextColor),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 19.5.sp
                        ),
                        color = baseTextColor
                    )
                }
            }
            i++
        }
    }
}

/**
 * Parses inline markdown elements (**bold**, *italic*, _italic_, `code`) into an AnnotatedString
 */
private fun parseInlineMarkdown(
    text: String,
    baseColor: Color,
    codeBgColor: Color,
    codeTextColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        val length = text.length

        while (idx < length) {
            when {
                // Bold: **text**
                text.startsWith("**", idx) -> {
                    val closeIdx = text.indexOf("**", idx + 2)
                    if (closeIdx != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = baseColor))
                        append(text.substring(idx + 2, closeIdx))
                        pop()
                        idx = closeIdx + 2
                    } else {
                        append(text[idx])
                        idx++
                    }
                }

                // Inline code: `text`
                text.startsWith("`", idx) -> {
                    val closeIdx = text.indexOf("`", idx + 1)
                    if (closeIdx != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                background = codeBgColor,
                                color = codeTextColor,
                                fontSize = 11.5.sp
                            )
                        )
                        append(" ${text.substring(idx + 1, closeIdx)} ")
                        pop()
                        idx = closeIdx + 1
                    } else {
                        append(text[idx])
                        idx++
                    }
                }

                // Italic: *text* (single asterisk, not followed by another asterisk)
                text.startsWith("*", idx) && !text.startsWith("**", idx) -> {
                    val closeIdx = text.indexOf("*", idx + 1)
                    if (closeIdx != -1 && (closeIdx + 1 >= length || text[closeIdx + 1] != '*')) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor))
                        append(text.substring(idx + 1, closeIdx))
                        pop()
                        idx = closeIdx + 1
                    } else {
                        append(text[idx])
                        idx++
                    }
                }

                // Italic: _text_
                text.startsWith("_", idx) -> {
                    val closeIdx = text.indexOf("_", idx + 1)
                    if (closeIdx != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseColor))
                        append(text.substring(idx + 1, closeIdx))
                        pop()
                        idx = closeIdx + 1
                    } else {
                        append(text[idx])
                        idx++
                    }
                }

                else -> {
                    append(text[idx])
                    idx++
                }
            }
        }
    }
}
