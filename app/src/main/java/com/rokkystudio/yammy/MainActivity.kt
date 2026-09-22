package com.rokkystudio.yammy

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var settingsStore: SettingsStore
    private lateinit var repository: ContentRepository
    private lateinit var backButton: ImageButton
    private lateinit var titleView: TextView
    private lateinit var navigationScroll: ScrollView
    private lateinit var navigationContainer: LinearLayout
    private lateinit var documentView: WebView
    private lateinit var themeToggleButton: ImageButton
    private lateinit var languageFlag: ImageButton

    private var currentFolder = ""
    private var documentOpened = false

    override fun attachBaseContext(newBase: Context) {
        val storedSettings = SettingsStore(newBase)
        val theme = storedSettings.getTheme()
        val locale = Locale.forLanguageTag(storedSettings.getLanguage())
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        val nightMode = when (theme) {
            AppTheme.LIGHT -> Configuration.UI_MODE_NIGHT_NO
            AppTheme.DARK -> Configuration.UI_MODE_NIGHT_YES
        }
        configuration.uiMode =
            (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode

        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsStore = SettingsStore(this)
        repository = ContentRepository(assets, settingsStore.getLanguage())

        backButton = findViewById(R.id.backButton)
        titleView = findViewById(R.id.titleView)
        navigationScroll = findViewById(R.id.navigationScroll)
        navigationContainer = findViewById(R.id.navigationContainer)
        documentView = findViewById(R.id.documentView)
        themeToggleButton = findViewById(R.id.themeToggleButton)
        languageFlag = findViewById(R.id.languageFlag)

        documentView.settings.javaScriptEnabled = false
        documentView.settings.allowFileAccess = false
        documentView.settings.allowContentAccess = false
        documentView.settings.domStorageEnabled = false
        documentView.settings.setSupportZoom(true)
        documentView.settings.builtInZoomControls = true
        documentView.settings.displayZoomControls = false
        documentView.setBackgroundColor(getColor(R.color.background))

        backButton.setOnClickListener { navigateBack() }
        themeToggleButton.setOnClickListener { toggleTheme() }
        languageFlag.setOnClickListener { showLanguageMenu(it) }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navigateBack()) finish()
            }
        })

        renderThemeToggle()
        renderLanguageFlag()
        showFolder(repository.rootPath)
    }

    override fun onDestroy() {
        documentView.stopLoading()
        documentView.removeAllViews()
        documentView.destroy()
        super.onDestroy()
    }

    private fun toggleTheme() {
        val next = when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> AppTheme.DARK
            AppTheme.DARK -> AppTheme.LIGHT
        }
        settingsStore.setTheme(next)
        recreate()
    }

    private fun renderThemeToggle() {
        when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> {
                themeToggleButton.setImageResource(R.drawable.theme_sun)
                themeToggleButton.contentDescription = getString(R.string.theme_light)
            }
            AppTheme.DARK -> {
                themeToggleButton.setImageResource(R.drawable.theme_moon)
                themeToggleButton.contentDescription = getString(R.string.theme_dark)
            }
        }
    }

    private fun showLanguageMenu(anchor: View) {
        val languages = repository.availableLanguages()
        if (languages.isEmpty()) return

        PopupMenu(this, anchor).apply {
            languages.forEachIndexed { index, language ->
                menu.add(0, index, index, language.name).apply {
                    isCheckable = true
                    isChecked = language.code == repository.languageCode
                }
            }

            setOnMenuItemClickListener { item ->
                val language = languages.getOrNull(item.itemId)
                    ?: return@setOnMenuItemClickListener false

                if (language.code != repository.languageCode) {
                    settingsStore.setLanguage(language.code)
                    recreate()
                }
                true
            }
            show()
        }
    }

    private fun renderLanguageFlag() {
        val language = repository.currentLanguage()
        val resourceId = resources.getIdentifier(
            "flag_${language.flag}",
            "drawable",
            packageName
        )
        languageFlag.setImageResource(
            if (resourceId != 0) resourceId else R.drawable.ic_language
        )
        languageFlag.contentDescription =
            getString(R.string.language_current, language.name)
    }

    private fun showFolder(path: String) {
        currentFolder = path
        documentOpened = false

        val metadata = repository.folderMetadata(path)
        titleView.text = metadata.title
        backButton.visibility = if (path == repository.rootPath) View.INVISIBLE else View.VISIBLE

        documentView.visibility = View.GONE
        navigationScroll.visibility = View.VISIBLE
        navigationContainer.removeAllViews()

        if (metadata.summary.isNotBlank()) {
            navigationContainer.addView(createIntro(metadata.summary))
        }

        repository.list(path).forEach { node ->
            navigationContainer.addView(createNavigationCard(node))
        }
    }

    private fun showDocument(path: String) {
        val metadata = repository.documentMetadata(path)
        val markdown = repository.readDocument(path)
        val html = MarkdownHtmlRenderer.render(
            markdown = markdown,
            title = metadata.title,
            languageCode = repository.languageCode,
            palette = markdownPalette()
        )

        documentOpened = true
        titleView.text = metadata.title
        backButton.visibility = View.VISIBLE
        navigationScroll.visibility = View.GONE
        documentView.visibility = View.VISIBLE

        documentView.loadDataWithBaseURL(
            "https://yammy.local/",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun navigateBack(): Boolean {
        if (documentOpened) {
            showFolder(currentFolder)
            return true
        }

        if (currentFolder == repository.rootPath) return false

        val parent = currentFolder.substringBeforeLast('/', repository.rootPath)
        showFolder(parent)
        return true
    }

    private fun createIntro(summary: String): TextView {
        return TextView(this).apply {
            text = summary
            setTextColor(getColor(R.color.text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setLineSpacing(0f, 1.2f)
            setPadding(dp(4), dp(2), dp(4), dp(16))
        }
    }

    private fun createNavigationCard(node: ContentNode): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                setColor(getColor(if (node.isDirectory) R.color.surface else R.color.surface_raised))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), getColor(R.color.border))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                if (node.isDirectory) showFolder(node.path) else showDocument(node.path)
            }
        }

        val icon = ImageView(this).apply {
            setImageResource(iconResource(node.icon, node.isDirectory))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40)).apply {
                marginEnd = dp(14)
            }
        }

        val textColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        textColumn.addView(
            TextView(this).apply {
                text = node.title
                setTextColor(getColor(R.color.text_primary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        )

        if (node.summary.isNotBlank()) {
            textColumn.addView(
                TextView(this).apply {
                    text = node.summary
                    setTextColor(getColor(R.color.text_secondary))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(4), 0, 0)
                }
            )
        }

        card.addView(icon)
        card.addView(textColumn)
        return card
    }

    private fun iconResource(icon: String, isDirectory: Boolean): Int {
        return when (icon.lowercase()) {
            "safe" -> R.drawable.ic_section_safe
            "risky" -> R.drawable.ic_section_risky
            "sugars" -> R.drawable.ic_section_sugars
            "tables" -> R.drawable.ic_section_tables
            "article" -> R.drawable.ic_article
            else -> if (isDirectory) R.drawable.ic_section_default else R.drawable.ic_article
        }
    }

    private fun markdownPalette(): MarkdownPalette {
        return MarkdownPalette(
            background = colorHex(R.color.background),
            surface = colorHex(R.color.surface_raised),
            header = colorHex(R.color.surface),
            border = colorHex(R.color.border),
            textPrimary = colorHex(R.color.text_primary)
        )
    }

    private fun colorHex(@ColorRes color: Int): String {
        return String.format("#%06X", 0xFFFFFF and getColor(color))
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
