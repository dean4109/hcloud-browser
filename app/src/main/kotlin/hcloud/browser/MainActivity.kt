package hcloud.browser

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView

class MainActivity : Activity() {

  companion object {
    const val HOME = "https://m.youtube.com/"
    private const val PREFS = "browser"
    private const val KEY_LAST_URL = "last_url"
    private const val KEY_DESKTOP = "desktop"
  }

  private lateinit var root: FrameLayout
  private lateinit var webView: WebView
  private lateinit var urlBar: EditText
  private lateinit var progress: ProgressBar
  private lateinit var desktopButton: TextView
  private lateinit var mobileUserAgent: String

  private var fullscreenView: View? = null
  private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null
  private var desktopMode = false

  private val prefs by lazy { getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    desktopMode = prefs.getBoolean(KEY_DESKTOP, false)
    buildUi()
    configureWebView()

    if (savedInstanceState != null) {
      webView.restoreState(savedInstanceState)
    } else {
      val start = intent?.data?.toString() ?: prefs.getString(KEY_LAST_URL, null) ?: HOME
      webView.loadUrl(start)
    }
  }

  // ---------- UI ----------

  private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

  private fun toolbarButton(label: String, onClick: () -> Unit) = TextView(this).apply {
    text = label
    textSize = 26f
    setTextColor(Color.WHITE)
    gravity = Gravity.CENTER
    minWidth = dp(64)
    setPadding(dp(12), 0, dp(12), 0)
    isClickable = true
    isFocusable = true
    setBackgroundResource(android.R.drawable.list_selector_background)
    setOnClickListener { onClick() }
  }

  private fun buildUi() {
    val toolbar = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      gravity = Gravity.CENTER_VERTICAL
      setBackgroundColor(Color.rgb(32, 33, 36))
      setPadding(dp(8), dp(6), dp(8), dp(6))
    }

    urlBar = EditText(this).apply {
      setSingleLine()
      textSize = 20f
      setTextColor(Color.WHITE)
      setHintTextColor(Color.GRAY)
      hint = "Search or type a web address"
      imeOptions = EditorInfo.IME_ACTION_GO
      inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
      setSelectAllOnFocus(true)
      setPadding(dp(16), dp(10), dp(16), dp(10))
      background = android.graphics.drawable.GradientDrawable().apply {
        cornerRadius = dp(24).toFloat()
        setColor(Color.rgb(60, 64, 67))
      }
      setOnEditorActionListener { _, actionId, event ->
        val enter = event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN
        if (actionId == EditorInfo.IME_ACTION_GO || enter) {
          navigate(text.toString()); true
        } else false
      }
    }

    desktopButton = toolbarButton("") { toggleDesktop() }.apply { textSize = 16f }
    updateDesktopLabel()

    toolbar.addView(toolbarButton("←") { if (webView.canGoBack()) webView.goBack() })
    toolbar.addView(toolbarButton("→") { if (webView.canGoForward()) webView.goForward() })
    toolbar.addView(toolbarButton("⟳") { webView.reload() })
    toolbar.addView(toolbarButton("⌂") { webView.loadUrl(HOME) })
    toolbar.addView(urlBar, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
      marginStart = dp(8); marginEnd = dp(8)
    })
    toolbar.addView(desktopButton)

    progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
      max = 100
      visibility = View.GONE
    }

    webView = WebView(this)

    val column = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      setBackgroundColor(Color.BLACK)
      addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)))
      addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)))
      addView(webView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    root = FrameLayout(this).apply { addView(column) }
    setContentView(root)
  }

  private fun updateDesktopLabel() {
    desktopButton.text = if (desktopMode) "Desktop" else "Mobile"
    desktopButton.typeface = Typeface.DEFAULT_BOLD
  }

  // ---------- WebView ----------

  @SuppressLint("SetJavaScriptEnabled")
  private fun configureWebView() {
    with(webView.settings) {
      javaScriptEnabled = true
      domStorageEnabled = true
      databaseEnabled = true
      mediaPlaybackRequiresUserGesture = false
      loadWithOverviewMode = true
      useWideViewPort = true
      builtInZoomControls = true
      displayZoomControls = false
      setSupportMultipleWindows(false)
      javaScriptCanOpenWindowsAutomatically = false
      mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
      // Drop the "wv" WebView marker so sites serve their normal Chrome pages.
      userAgentString = userAgentString.replace("; wv", "").replace(Regex(" Version/\\d+\\.\\d+"), "")
      mobileUserAgent = userAgentString
    }
    applyUserAgent()

    CookieManager.getInstance().apply {
      setAcceptCookie(true)
      setAcceptThirdPartyCookies(webView, true)
    }

    webView.webViewClient = object : WebViewClient() {
      override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val scheme = request.url.scheme?.lowercase()
        return scheme != "http" && scheme != "https" // ignore intent://, market://, etc.
      }

      override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        if (!urlBar.hasFocus()) urlBar.setText(url)
      }

      override fun onPageFinished(view: WebView, url: String) {
        if (!urlBar.hasFocus()) urlBar.setText(url)
        prefs.edit().putString(KEY_LAST_URL, url).apply()
        CookieManager.getInstance().flush()
      }
    }

    webView.webChromeClient = object : WebChromeClient() {
      override fun onProgressChanged(view: WebView, newProgress: Int) {
        progress.progress = newProgress
        progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
      }

      override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        if (fullscreenView != null) { callback.onCustomViewHidden(); return }
        fullscreenView = view
        fullscreenCallback = callback
        view.setBackgroundColor(Color.BLACK)
        root.addView(view, FrameLayout.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setImmersive(true)
      }

      override fun onHideCustomView() = exitFullscreen()
    }
  }

  private fun applyUserAgent() {
    webView.settings.userAgentString = if (desktopMode) {
      mobileUserAgent.replace(Regex("\\(Linux; Android [^)]*\\)"), "(X11; Linux x86_64)")
        .replace(" Mobile", "")
    } else mobileUserAgent
  }

  private fun toggleDesktop() {
    desktopMode = !desktopMode
    prefs.edit().putBoolean(KEY_DESKTOP, desktopMode).apply()
    updateDesktopLabel()
    applyUserAgent()
    val url = webView.url ?: HOME
    // YouTube uses separate mobile/desktop hosts; swap so the toggle takes effect.
    val swapped = when {
      desktopMode -> url.replace("://m.youtube.com", "://www.youtube.com")
      else -> url.replace("://www.youtube.com", "://m.youtube.com")
    }
    webView.loadUrl(swapped)
  }

  private fun exitFullscreen() {
    val view = fullscreenView ?: return
    root.removeView(view)
    fullscreenView = null
    fullscreenCallback?.onCustomViewHidden()
    fullscreenCallback = null
    setImmersive(false)
  }

  @Suppress("DEPRECATION")
  private fun setImmersive(on: Boolean) {
    window.insetsController?.let { c ->
      if (on) {
        c.hide(WindowInsets.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      } else {
        c.show(WindowInsets.Type.systemBars())
      }
    }
  }

  private fun navigate(input: String) {
    val text = input.trim()
    if (text.isEmpty()) return
    val url = when {
      text.startsWith("http://") || text.startsWith("https://") -> text
      !text.contains(' ') && text.contains('.') -> "https://$text"
      else -> "https://www.google.com/search?q=" + Uri.encode(text)
    }
    webView.loadUrl(url)
    urlBar.clearFocus()
    (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
      .hideSoftInputFromWindow(urlBar.windowToken, 0)
    webView.requestFocus()
  }

  // ---------- Lifecycle ----------

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    intent.data?.let { webView.loadUrl(it.toString()) }
  }

  @Deprecated("Deprecated in Java")
  override fun onBackPressed() {
    when {
      fullscreenView != null -> exitFullscreen()
      webView.canGoBack() -> webView.goBack()
      else -> @Suppress("DEPRECATION") super.onBackPressed()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    webView.saveState(outState)
  }

  override fun onPause() {
    super.onPause()
    webView.onPause()
    CookieManager.getInstance().flush()
  }

  override fun onResume() {
    super.onResume()
    webView.onResume()
  }

  override fun onDestroy() {
    webView.destroy()
    super.onDestroy()
  }
}
