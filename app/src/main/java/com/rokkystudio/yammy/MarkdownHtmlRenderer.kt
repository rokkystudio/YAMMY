package com.rokkystudio.yammy

data class MarkdownPalette(
    val background: String,
    val surface: String,
    val header: String,
    val border: String,
    val textPrimary: String
)

/**
 * Presentation adapter for standard Markdown stored in assets.
 *
 * The content files contain ordinary Markdown only. This class exists because Android WebView
 * renders HTML, not Markdown, so the app converts the small Markdown subset used by YAMMY to HTML.
 */
object MarkdownHtmlRenderer {

    fun render(markdown: String, title: String, languageCode: String, palette: MarkdownPalette): String {
        val lines = markdown.lines()
        val body = StringBuilder()
        var index = 0
        var listOpen = false

        while (index < lines.size) {
            val trimmed = lines[index].trim()

            if (trimmed.isEmpty()) {
                if (listOpen) {
                    body.append("</ul>")
                    listOpen = false
                }
                index++
                continue
            }

            if (isTableStart(lines, index)) {
                if (listOpen) {
                    body.append("</ul>")
                    listOpen = false
                }
                index = appendTable(lines, index, body)
                continue
            }

            when {
                trimmed.startsWith("### ") -> {
                    closeList(body, listOpen)
                    listOpen = false
                    body.append("<h3>").append(inline(trimmed.removePrefix("### "))).append("</h3>")
                }
                trimmed.startsWith("## ") -> {
                    closeList(body, listOpen)
                    listOpen = false
                    body.append("<h2>").append(inline(trimmed.removePrefix("## "))).append("</h2>")
                }
                trimmed.startsWith("# ") -> {
                    closeList(body, listOpen)
                    listOpen = false
                    body.append("<h1>").append(inline(trimmed.removePrefix("# "))).append("</h1>")
                }
                trimmed.startsWith("- ") -> {
                    if (!listOpen) {
                        body.append("<ul>")
                        listOpen = true
                    }
                    body.append("<li>").append(inline(trimmed.removePrefix("- "))).append("</li>")
                }
                else -> {
                    if (listOpen) {
                        body.append("</ul>")
                        listOpen = false
                    }
                    body.append("<p>").append(inline(trimmed)).append("</p>")
                }
            }
            index++
        }

        if (listOpen) body.append("</ul>")

        return """
            <!doctype html>
            <html lang="${escape(languageCode)}">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>${escape(title)}</title>
                <style>
                    :root { font-family: sans-serif; }
                    body {
                        margin: 0;
                        padding: 20px 18px 40px;
                        color: ${palette.textPrimary};
                        background: ${palette.background};
                        line-height: 1.55;
                        font-size: 16px;
                    }
                    h1 { margin: 0 0 18px; font-size: 28px; line-height: 1.2; }
                    h2 { margin: 28px 0 12px; font-size: 22px; }
                    h3 { margin: 22px 0 10px; font-size: 18px; }
                    p { margin: 10px 0; }
                    ul { margin: 10px 0; padding-left: 22px; }
                    .table-wrap {
                        overflow-x: auto;
                        margin: 14px 0 20px;
                        border: 1px solid ${palette.border};
                        border-radius: 12px;
                        background: ${palette.surface};
                    }
                    table { width: 100%; min-width: 620px; border-collapse: collapse; }
                    th, td {
                        padding: 12px 14px;
                        text-align: left;
                        vertical-align: top;
                        border-bottom: 1px solid ${palette.border};
                        white-space: nowrap;
                    }
                    th { background: ${palette.header}; font-weight: 700; }
                    tr:last-child td { border-bottom: 0; }
                    code {
                        padding: 2px 5px;
                        border-radius: 5px;
                        background: ${palette.header};
                        font-family: monospace;
                    }
                </style>
            </head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }

    private fun isTableStart(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size || !lines[index].contains('|')) return false

        val separator = lines[index + 1].trim()
        return separator.contains('|') &&
            separator.split('|')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .all { it.matches(Regex(":?-{3,}:?")) }
    }

    private fun appendTable(lines: List<String>, start: Int, body: StringBuilder): Int {
        val headers = tableCells(lines[start])
        body.append("<div class=\"table-wrap\"><table><thead><tr>")
        headers.forEach { body.append("<th>").append(inline(it)).append("</th>") }
        body.append("</tr></thead><tbody>")

        var index = start + 2
        while (index < lines.size) {
            val line = lines[index]
            if (line.isBlank() || !line.contains('|')) break

            val cells = tableCells(line)
            body.append("<tr>")
            headers.indices.forEach { cellIndex ->
                body.append("<td>")
                    .append(inline(cells.getOrElse(cellIndex) { "" }))
                    .append("</td>")
            }
            body.append("</tr>")
            index++
        }

        body.append("</tbody></table></div>")
        return index
    }

    private fun tableCells(line: String): List<String> {
        return line.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split('|')
            .map { it.trim() }
    }

    private fun inline(text: String): String {
        var value = escape(text)
        value = value.replace(Regex("\\*\\*(.+?)\\*\\*")) {
            "<strong>${it.groupValues[1]}</strong>"
        }
        value = value.replace(Regex("\u0060([^\u0060]+)\u0060")) {
            "<code>${it.groupValues[1]}</code>"
        }
        return value
    }

    private fun escape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }

    private fun closeList(body: StringBuilder, listOpen: Boolean) {
        if (listOpen) body.append("</ul>")
    }
}
