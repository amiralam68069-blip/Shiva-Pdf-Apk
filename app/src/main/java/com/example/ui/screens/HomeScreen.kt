package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.NavigationTab
import com.example.ui.theme.*
import com.example.web.ShivaWebChromeClient
import com.example.web.ShivaWebViewClient
import com.example.web.WebActionListener
import com.example.web.WebHelper

internal fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onOpenDownloads: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }

    val currentTab by viewModel.currentTab.collectAsState()
    val isWebLoading by viewModel.isWebLoading.collectAsState()
    val webProgress by viewModel.webProgress.collectAsState()
    val hasWebError by viewModel.hasWebError.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val downloadedPdfs by viewModel.downloadedPdfs.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var detectedPdfUrl by remember { mutableStateOf<String?>(null) }
    var detectedPdfTitle by remember { mutableStateOf("") }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showSearchOverlay by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    // File Chooser for Web uploads
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
            filePathCallback?.onReceiveValue(uris)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // Handle back button navigation inside WebView
    BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    var lastLoadedTab by remember { mutableStateOf<NavigationTab?>(null) }
    // Load URL cleanly when tab changes or when webViewInstance is ready
    LaunchedEffect(currentTab, webViewInstance) {
        val webView = webViewInstance ?: return@LaunchedEffect
        if (lastLoadedTab != currentTab) {
            lastLoadedTab = currentTab
            val targetUrl = "https://sivapdf.free.je${currentTab.path}"
            webView.loadUrl(targetUrl)
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ShivaSurface)
            ) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, PurplePrimary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_shiva_logo),
                                    contentDescription = "SHIVA PDF",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SHIVA ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextWhite,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "PDF",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = AmberGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (isOnline) "Educational Portal" else "Offline Mode",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnline) PurpleLight else AmberGold
                                )
                            }
                        }
                    },
                    actions = {
                        // Search Button
                        IconButton(onClick = { showSearchOverlay = !showSearchOverlay }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (showSearchOverlay) PurpleLight else TextWhite
                            )
                        }

                        // Downloads / Offline Library Button with Badge
                        IconButton(onClick = onOpenDownloads) {
                            BadgedBox(
                                badge = {
                                    if (downloadedPdfs.isNotEmpty()) {
                                        Badge(
                                            containerColor = PurplePrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text(downloadedPdfs.size.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DownloadDone,
                                    contentDescription = "Downloads",
                                    tint = TextWhite
                                )
                            }
                        }

                        // Reload Button
                        IconButton(onClick = {
                            viewModel.clearWebError()
                            webViewInstance?.reload()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = TextWhite
                            )
                        }

                        // More Options Menu
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options",
                                    tint = TextWhite
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.background(ShivaCard)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("WhatsApp Support", color = TextWhite) },
                                    leadingIcon = {
                                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = GreenSuccess)
                                    },
                                    onClick = {
                                        showMenu = false
                                        openWhatsAppSupport(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Share App", color = TextWhite) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null, tint = PurpleLight)
                                    },
                                    onClick = {
                                        showMenu = false
                                        shareShivaPdfApp(context)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Clear Cache", color = TextWhite) },
                                    leadingIcon = {
                                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = AmberGold)
                                    },
                                    onClick = {
                                        showMenu = false
                                        webViewInstance?.clearCache(true)
                                        Toast.makeText(context, "App cache cleared", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ShivaSurface)
                )

                // Search Overlay Bar
                AnimatedVisibility(visible = showSearchOverlay) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text("Search Class, Subject or Notes...", color = TextMuted) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                if (searchInput.isNotBlank()) {
                                    IconButton(onClick = {
                                        val searchUrl = "https://sivapdf.free.je/pdfs.php?search=${Uri.encode(searchInput)}"
                                        webViewInstance?.loadUrl(searchUrl)
                                        showSearchOverlay = false
                                    }) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = "Search", tint = PurplePrimary)
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = ShivaBorder,
                                focusedContainerColor = ShivaCard,
                                unfocusedContainerColor = ShivaCard,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite
                            )
                        )
                    }
                }

                // Quick Category Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryChip(title = "All Notes", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php")
                    })
                    CategoryChip(title = "Free 🆓", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php?type=free")
                    })
                    CategoryChip(title = "Premium 👑", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php?type=premium")
                    })
                    CategoryChip(title = "Class 10th", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php?class=10th")
                    })
                    CategoryChip(title = "Class 12th", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php?class=12th")
                    })
                    CategoryChip(title = "Class 9th", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php?class=9th")
                    })
                    CategoryChip(title = "Polytechnic", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php?category=polytechnic")
                    })
                    CategoryChip(title = "Competitive", onClick = {
                        webViewInstance?.loadUrl("https://sivapdf.free.je/pdfs.php?category=competitive")
                    })
                }

                // Loading progress bar
                if (isWebLoading) {
                    LinearProgressIndicator(
                        progress = { webProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = PurplePrimary,
                        trackColor = ShivaBackground
                    )
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = ShivaSurface,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    NavigationTab.HOME -> Icons.Default.Home
                                    NavigationTab.PDFS -> Icons.Default.MenuBook
                                    NavigationTab.QUIZ -> Icons.Default.Quiz
                                    NavigationTab.PURCHASES -> Icons.Default.ShoppingBag
                                    NavigationTab.PROFILE -> Icons.Default.Person
                                },
                                contentDescription = tab.label
                            )
                        },
                        label = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = PurpleLight,
                            indicatorColor = PurplePrimary,
                            unselectedIconColor = TextGray,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        },
        containerColor = ShivaBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Web View
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        WebHelper.configureWebView(this, ctx)

                        val actionListener = object : WebActionListener {
                            override fun onPageLoadProgress(progress: Int) {
                                viewModel.setWebLoading(progress < 100, progress)
                            }

                            override fun onPageStarted(url: String) {
                                viewModel.setWebLoading(true, 10)
                            }

                            override fun onPageFinished(url: String) {
                                viewModel.setWebLoading(false, 100)
                            }

                            override fun onPageError(
                                errorCode: Int,
                                description: String,
                                failingUrl: String
                            ) {
                                viewModel.setWebError(description)
                            }

                            override fun onPdfUrlDetected(
                                pdfUrl: String,
                                suggestedTitle: String
                            ) {
                                detectedPdfUrl = pdfUrl
                                detectedPdfTitle = suggestedTitle
                                showDownloadDialog = true
                            }

                            override fun onExternalIntent(intent: Intent) {
                                try {
                                    ctx.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(ctx, "App not found to handle action", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        webViewClient = ShivaWebViewClient(ctx, actionListener)
                        webChromeClient = ShivaWebChromeClient(
                            activity = activity,
                            actionListener = actionListener,
                            fileChooserLauncher = { callback, params ->
                                filePathCallback = callback
                                val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                }
                                fileChooserLauncher.launch(intent)
                            }
                        )

                        setBackgroundColor(android.graphics.Color.parseColor("#080B14"))
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )

            // Offline / Connection Error Overlay
            if (hasWebError || (!isOnline && isWebLoading)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ShivaBackground)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = ShivaCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ShivaBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = RedError.copy(alpha = 0.15f),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.WifiOff,
                                        contentDescription = null,
                                        tint = RedError,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Unable to Connect",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextWhite,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Please check your internet connection or Wi-Fi network and try again.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    viewModel.clearWebError()
                                    webViewInstance?.reload()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Retry Connection")
                            }

                            if (downloadedPdfs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onOpenDownloads,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = PurpleLight)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Offline Notes (${downloadedPdfs.size})")
                                }
                            }
                        }
                    }
                }
            }

            // Downloading Banner Toast / Overlay
            AnimatedVisibility(
                visible = downloadStatus.isDownloading,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ShivaSurface.copy(alpha = 0.95f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PurplePrimary),
                    shadowElevation = 10.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { downloadStatus.progress / 100f },
                            modifier = Modifier.size(36.dp),
                            color = PurplePrimary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloading PDF...",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite
                            )
                            Text(
                                text = "${downloadStatus.progress}% • ${downloadStatus.title}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PurpleLight,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // PDF Action Dialog (Read In-App vs Download)
    if (showDownloadDialog && detectedPdfUrl != null) {
        val pdfUrl = detectedPdfUrl!!
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = AmberGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Educational PDF", color = TextWhite)
                }
            },
            text = {
                Column {
                    Text(
                        text = detectedPdfTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Would you like to open and read this study document in the in-app viewer or download it for offline access?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadDialog = false
                        viewModel.downloadPdf(
                            pdfUrl = pdfUrl,
                            title = detectedPdfTitle,
                            openAfterDownload = true
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    Icon(imageVector = Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read In-App")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            showDownloadDialog = false
                            viewModel.downloadPdf(
                                pdfUrl = pdfUrl,
                                title = detectedPdfTitle,
                                openAfterDownload = false
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download")
                    }
                }
            },
            containerColor = ShivaSurface
        )
    }
}

@Composable
private fun CategoryChip(title: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ShivaCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, ShivaBorder),
        modifier = Modifier.padding(end = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 36.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextWhite
                )
            }
        }
    }
}

private fun openWhatsAppSupport(context: Context) {
    try {
        val uri = Uri.parse("https://wa.me/?text=Hello%20SHIVA%20PDF%20Team%2C%20I%20need%20assistance%20with%20study%20notes.")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "WhatsApp is not installed", Toast.LENGTH_SHORT).show()
    }
}

private fun shareShivaPdfApp(context: Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "SHIVA PDF - Educational Notes & Quiz")
        putExtra(
            Intent.EXTRA_TEXT,
            "Download SHIVA PDF App for free and premium educational notes, PDFs and Quizzes: https://sivapdf.free.je"
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share SHIVA PDF"))
}
