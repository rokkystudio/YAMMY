package com.rokkystudio.yammy

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import badfox.logic.neoui.NeoTopBarView

/**
 * Displays the localized YAMMY content tree, renders Markdown articles and routes shared NeoUI theme and language controls.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var settingsStore: SettingsStore
    private lateinit var repository: ContentRepository
    private lateinit var topBar: NeoTopBarView
    private lateinit var backButton: ImageButton
    private lateinit var titleView: TextView
    private lateinit var navigationScroll: ScrollView
    private lateinit var navigationContainer: LinearLayout
    private lateinit var documentView: WebView

    private var currentFolder = ""
    private var documentOpened = false

    override fun attachBaseContext(newBase: Context) {
        val configuration = Configuration(newBase.resources.configuration)
        SettingsStore(newBase).applyToConfiguration(configuration)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SettingsStore(this).applyAppCompatNightMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsStore = SettingsStore(this)
        repository = ContentRepository(assets, settingsStore.getLanguage())

        topBar = findViewById(R.id.topBar)
        backButton = findViewById(R.id.backButton)
        titleView = findViewById(R.id.titleView)
        navigationScroll = findViewById(R.id.navigationScroll)
        navigationContainer = findViewById(R.id.navigationContainer)
        documentView = findViewById(R.id.documentView)

        applyExplicitTheme()
        configureTopBar()

        documentView.settings.javaScriptEnabled = false
        documentView.settings.allowFileAccess = false
        documentView.settings.allowContentAccess = false
        documentView.settings.domStorageEnabled = false
        documentView.settings.setSupportZoom(true)
        documentView.settings.builtInZoomControls = true
        documentView.settings.displayZoomControls = false
        documentView.setBackgroundColor(dynamicBackgroundColor())

        backButton.setOnClickListener { navigateBack() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!navigateBack()) finish()
            }
        })

        showFolder(repository.rootPath)
    }

    override fun onDestroy() {
        documentView.stopLoading()
        documentView.removeAllViews()
        documentView.destroy()
        super.onDestroy()
    }

    /**
     * Configures the shared top bar with YAMMY identity, app theme options and language options.
     */
    private fun configureTopBar() {
        topBar.setAppInfo(
            title = getString(R.string.app_name),
            subtitle = getString(R.string.app_subtitle),
            iconResId = R.mipmap.ic_launcher
        )
        settingsStore.bindTopBarUi(topBar)
    }

    private fun showFolder(path: String) {
        currentFolder = path
        documentOpened = false

        val metadata = repository.folderMetadata(path)
        titleView.text = metadata.title
        titleView.visibility = View.VISIBLE
        backButton.visibility = if (path == repository.rootPath) View.GONE else View.VISIBLE

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
        titleView.visibility = View.GONE
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
            setTextColor(dynamicTextSecondary())
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
                setColor(dynamicCardColor(node.isDirectory))
                cornerRadius = dp(12).toFloat()
                setStroke(dp(1), dynamicBorderColor())
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
                setTextColor(dynamicTextPrimary())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
        )

        if (node.summary.isNotBlank()) {
            textColumn.addView(
                TextView(this).apply {
                    text = node.summary
                    setTextColor(dynamicTextSecondary())
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
        return when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> MarkdownPalette(
                background = "#E0E0E0",
                surface = "#F0F0F0",
                header = "#F0F0F0",
                border = "#E2E4E8",
                textPrimary = "#191A1C"
            )
            AppTheme.DARK -> MarkdownPalette(
                background = "#111418",
                surface = "#1D232A",
                header = "#222732",
                border = "#2A323B",
                textPrimary = "#F2F5F8"
            )
        }
    }

    private fun applyExplicitTheme() {
        val background = dynamicBackgroundColor()

        findViewById<View>(R.id.rootLayout).setBackgroundColor(background)
        findViewById<View>(R.id.contentHeader).setBackgroundColor(background)
        titleView.setTextColor(dynamicTextPrimary())
        navigationScroll.setBackgroundColor(background)
        documentView.setBackgroundColor(background)
    }

    private fun dynamicBackgroundColor(): Int {
        return when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> Color.rgb(224, 224, 224)
            AppTheme.DARK -> Color.rgb(17, 20, 24)
        }
    }

    private fun dynamicCardColor(isDirectory: Boolean): Int {
        return when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> Color.rgb(240, 240, 240)
            AppTheme.DARK -> if (isDirectory) Color.rgb(34, 39, 50) else Color.rgb(29, 35, 42)
        }
    }

    private fun dynamicBorderColor(): Int {
        return when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> Color.rgb(226, 228, 232)
            AppTheme.DARK -> Color.rgb(42, 50, 59)
        }
    }

    private fun dynamicTextPrimary(): Int {
        return when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> Color.rgb(25, 26, 28)
            AppTheme.DARK -> Color.rgb(242, 245, 248)
        }
    }

    private fun dynamicTextSecondary(): Int {
        return when (settingsStore.getTheme()) {
            AppTheme.LIGHT -> Color.rgb(81, 82, 85)
            AppTheme.DARK -> Color.rgb(174, 178, 182)
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
