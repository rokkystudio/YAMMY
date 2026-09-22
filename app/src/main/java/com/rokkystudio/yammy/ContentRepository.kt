package com.rokkystudio.yammy

import android.content.res.AssetManager
import java.util.Locale

data class ContentMetadata(
    val title: String,
    val summary: String,
    val icon: String
)

data class ContentNode(
    val path: String,
    val title: String,
    val summary: String,
    val icon: String,
    val isDirectory: Boolean
)

data class ContentLanguage(
    val code: String,
    val name: String,
    val flag: String
)

class ContentRepository(
    private val assets: AssetManager,
    val languageCode: String
) {
    val rootPath: String
        get() = "${CONTENT_ROOT}/${languageCode}"

    fun availableLanguages(): List<ContentLanguage> {
        return assets.list(CONTENT_ROOT)
            .orEmpty()
            .filter { code ->
                code.matches(Regex("^[a-z]{2,8}$")) &&
                    assets.list("${CONTENT_ROOT}/${code}").orEmpty().isNotEmpty()
            }
            .map { code ->
                val locale = Locale.forLanguageTag(code)
                val displayName = locale.getDisplayLanguage(locale)
                    .replaceFirstChar { character ->
                        if (character.isLowerCase()) character.titlecase(locale) else character.toString()
                    }
                    .ifBlank { code.uppercase() }

                ContentLanguage(
                    code = code,
                    name = displayName,
                    flag = code
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun currentLanguage(): ContentLanguage {
        return availableLanguages().firstOrNull { it.code == languageCode }
            ?: ContentLanguage(languageCode, languageCode.uppercase(), languageCode)
    }

    fun list(path: String): List<ContentNode> {
        return assets.list(path)
            .orEmpty()
            .filter { it != README_FILE }
            .mapNotNull { name ->
                val childPath = "${path}/${name}"
                val children = assets.list(childPath).orEmpty()
                val isDirectory = children.isNotEmpty()

                if (!isDirectory && !name.endsWith(".md", ignoreCase = true)) {
                    return@mapNotNull null
                }

                val metadataPath = if (isDirectory) {
                    "${childPath}/${README_FILE}"
                } else {
                    childPath
                }

                val metadata = readMetadata(
                    path = metadataPath,
                    fallbackTitle = fallbackTitle(name),
                    icon = iconFor(name, isDirectory)
                )

                ContentNode(
                    path = childPath,
                    title = metadata.title,
                    summary = metadata.summary,
                    icon = metadata.icon,
                    isDirectory = isDirectory
                )
            }
            .sortedWith(
                compareByDescending<ContentNode> { it.isDirectory }
                    .thenBy { it.title.lowercase() }
            )
    }

    fun folderMetadata(path: String): ContentMetadata {
        val folderName = path.substringAfterLast('/')
        return readMetadata(
            path = "${path}/${README_FILE}",
            fallbackTitle = fallbackTitle(folderName),
            icon = iconFor(folderName, true)
        )
    }

    fun documentMetadata(path: String): ContentMetadata {
        val fileName = path.substringAfterLast('/')
        return readMetadata(
            path = path,
            fallbackTitle = fallbackTitle(fileName),
            icon = "article"
        )
    }

    fun readDocument(path: String): String {
        return assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun readMetadata(path: String, fallbackTitle: String, icon: String): ContentMetadata {
        val markdown = runCatching { readDocument(path) }.getOrNull().orEmpty()

        return ContentMetadata(
            title = extractTitle(markdown).ifBlank { fallbackTitle },
            summary = extractSummary(markdown),
            icon = icon
        )
    }

    private fun extractTitle(markdown: String): String {
        return markdown.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("# ") }
            ?.removePrefix("# ")
            ?.trim()
            .orEmpty()
    }

    private fun extractSummary(markdown: String): String {
        val paragraph = mutableListOf<String>()

        for (rawLine in markdown.lines()) {
            val line = rawLine.trim()

            if (line.isBlank()) {
                if (paragraph.isNotEmpty()) break
                continue
            }

            val isStructural = line.startsWith("#") ||
                line.startsWith("|") ||
                line.startsWith("- ") ||
                line.startsWith("* ") ||
                line.startsWith(">") ||
                line.startsWith("```")

            if (isStructural) {
                if (paragraph.isNotEmpty()) break
                continue
            }

            paragraph += line
        }

        return paragraph.joinToString(" ")
    }

    private fun iconFor(name: String, isDirectory: Boolean): String {
        if (!isDirectory) return "article"

        return when (name) {
            "safe_foods" -> "safe"
            "risky_foods" -> "risky"
            "sugars_sweeteners" -> "sugars"
            "comparison_tables" -> "tables"
            else -> "section"
        }
    }

    private fun fallbackTitle(name: String): String {
        return name
            .removeSuffix(".md")
            .replace('_', ' ')
            .trim()
    }

    companion object {
        private const val CONTENT_ROOT = "content"
        private const val README_FILE = "readme.md"
    }
}
