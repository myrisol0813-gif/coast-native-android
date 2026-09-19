package com.elementeracoast.app.feature.gate

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.elementeracoast.app.BuildConfig
import com.elementeracoast.app.core.network.CoastApiConfig

data class MailboxWebTarget(
    val origin: String,
    val mailboxUrl: String
) {
    companion object {
        fun production(): MailboxWebTarget {
            val config = CoastApiConfig.production()
            return MailboxWebTarget(
                origin = config.origin,
                mailboxUrl = config.url("/mailbox")
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
fun MailboxWebSurface(
    target: MailboxWebTarget = MailboxWebTarget.production(),
    onClose: () -> Unit
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    BackHandler {
        val current = webView
        if (current?.canGoBack() == true) current.goBack() else onClose()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = GateVisualTokens.ScreenHorizontalPadding,
                        vertical = GateVisualTokens.MailboxWebTopPadding
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("海岸信箱", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (loading) "正在沿海岸打开…" else "真实访客房间 · 与 PWA 同源",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                TextButton(onClick = onClose) { Text("关闭") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            error?.let { message ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(GateVisualTokens.ScreenHorizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(GateVisualTokens.MailboxWebErrorGap)
                ) {
                    Text(
                        message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = {
                        error = null
                        loading = true
                        webView?.loadUrl(target.mailboxUrl)
                    }) {
                        Text("重新打开信箱")
                    }
                }
            }
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, false)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            userAgentString = "$userAgentString ElementeraCoastNative/${BuildConfig.VERSION_NAME}"
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val allowed = request.url.isSameCoastOrigin(target.origin)
                                if (!allowed && request.isForMainFrame) {
                                    error = "海岸信箱阻止了外部页面跳转；这里只允许打开 Elementera Coast 自己的信箱页面。"
                                }
                                return !allowed
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                if (url?.let { Uri.parse(it).isSameCoastOrigin(target.origin) } == true) {
                                    loading = true
                                    error = null
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                CookieManager.getInstance().flush()
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                webError: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
                                    error = "海岸信箱没有打开：${webError?.description ?: "网络连接失败"}。"
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?
                            ) {
                                if (request?.isForMainFrame == true && (errorResponse?.statusCode ?: 0) >= 400) {
                                    loading = false
                                    error = when (errorResponse?.statusCode) {
                                        401 -> "信箱访客登录态已失效，请重新输入暗号。"
                                        403 -> "当前访客没有打开这个信箱页面的权限。"
                                        503 -> "海岸信箱后端暂时不可用，请稍后重试。"
                                        else -> "海岸信箱接口返回 HTTP ${errorResponse?.statusCode ?: "错误"}。"
                                    }
                                }
                            }
                        }
                        loadUrl(target.mailboxUrl)
                    }
                }
            )
        }
    }
}

private fun Uri.isSameCoastOrigin(origin: String): Boolean {
    val expected = runCatching { Uri.parse(origin) }.getOrNull() ?: return false
    return scheme.equals(expected.scheme, ignoreCase = true)
        && host.equals(expected.host, ignoreCase = true)
        && effectivePort() == expected.effectivePort()
}

private fun Uri.effectivePort(): Int = when {
    port >= 0 -> port
    scheme.equals("https", ignoreCase = true) -> 443
    scheme.equals("http", ignoreCase = true) -> 80
    else -> -1
}
