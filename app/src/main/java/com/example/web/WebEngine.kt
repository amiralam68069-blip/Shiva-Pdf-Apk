package com.example.web

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import java.net.URLDecoder

interface WebActionListener {
    fun onPageLoadProgress(progress: Int)
    fun onPageStarted(url: String)
    fun onPageFinished(url: String)
    fun onPageError(errorCode: Int, description: String, failingUrl: String)
    fun onPdfUrlDetected(pdfUrl: String, suggestedTitle: String)
    fun onExternalIntent(intent: Intent)
}

class ShivaWebChromeClient(
    private val activity: Activity? = null,
    private val actionListener: WebActionListener,
    private val fileChooserLauncher: ((ValueCallback<Array<Uri>>?, WebChromeClient.FileChooserParams?) -> Unit)? = null
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        actionListener.onPageLoadProgress(newProgress)
    }

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        return if (fileChooserLauncher != null) {
            fileChooserLauncher.invoke(filePathCallback, fileChooserParams)
            true
        } else {
            super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
        }
    }

    override fun onJsAlert(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // Let Android handle standard dialog or Toast
        Toast.makeText(activity, message ?: "", Toast.LENGTH_LONG).show()
        result?.confirm()
        return true
    }

    override fun onJsConfirm(
        view: WebView?,
        url: String?,
        message: String?,
        result: JsResult?
    ): Boolean {
        // Confirm default
        result?.confirm()
        return true
    }
}

class ShivaWebViewClient(
    private val context: Context,
    private val actionListener: WebActionListener
) : WebViewClient() {

    private val allowedDomains = listOf(
        "sivapdf.free.je",
        "free.je",
        "accounts.google.com",
        "paytm.com",
        "razorpay.com",
        "phonepe.com",
        "cashfree.com",
        "i.ibb.co",
        "fonts.googleapis.com",
        "fonts.gstatic.com"
    )

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        return handleUrl(view, url)
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        if (url == null) return false
        return handleUrl(view, url)
    }

    private fun handleUrl(view: WebView?, url: String): Boolean {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase() ?: ""

        // Handle external protocols (WhatsApp, UPI, Tel, Mailto)
        if (scheme in listOf("tel", "mailto", "whatsapp", "upi", "market", "intent")) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                actionListener.onExternalIntent(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot open external application", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        // Check if PDF URL
        if (isPdfUrl(url)) {
            val suggestedTitle = extractTitleFromUrl(url)
            actionListener.onPdfUrlDetected(url, suggestedTitle)
            return true
        }

        // Check domain security
        val host = uri.host?.lowercase() ?: ""
        val isAllowed = allowedDomains.any { allowed -> host.endsWith(allowed) }

        if (!isAllowed && (scheme == "http" || scheme == "https")) {
            // Open external domain in browser for security
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            } catch (_: Exception) {
            }
            return true
        }

        return false
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (url != null) {
            actionListener.onPageStarted(url)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        CookieManager.getInstance().flush()
        if (url != null) {
            actionListener.onPageFinished(url)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error?.errorCode ?: -1
            } else {
                -1
            }
            val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                error?.description?.toString() ?: "Network error"
            } else {
                "Network error"
            }
            actionListener.onPageError(errorCode, description, request.url.toString())
        }
    }

    private fun isPdfUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.endsWith(".pdf") ||
                lower.contains(".pdf?") ||
                lower.contains("download_pdf") ||
                lower.contains("view_pdf") ||
                lower.contains("action=download")
    }

    private fun extractTitleFromUrl(url: String): String {
        return try {
            val path = Uri.parse(url).lastPathSegment ?: "Document"
            val decoded = URLDecoder.decode(path, "UTF-8")
            if (decoded.endsWith(".pdf", ignoreCase = true)) {
                decoded.substringBeforeLast(".")
            } else {
                decoded
            }
        } catch (_: Exception) {
            "SHIVA PDF Notes"
        }
    }
}

object WebHelper {
    @SuppressLint("SetJavaScriptEnabled")
    fun configureWebView(webView: WebView, context: Context) {
        // Prevent Mesa DRI rendernode errors in virtualized/cloud emulator environments
        try {
            val hasHardwareRenderNode = java.io.File("/dev/dri/renderD128").exists()
            if (!hasHardwareRenderNode) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            }
        } catch (_: Exception) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.allowFileAccess = false
        settings.allowContentAccess = true
        settings.loadsImagesAutomatically = true
        settings.savePassword = false
        settings.setGeolocationEnabled(false)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // Custom User Agent for high compatibility with modern mobile web and aes.js
        val defaultUa = settings.userAgentString
        settings.userAgentString = "$defaultUa ShivaPdfApp/1.0"

        // Setup CookieManager
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
    }
}
