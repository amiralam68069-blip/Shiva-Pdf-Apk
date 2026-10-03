package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PdfReaderScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.ShivaBackground
import com.example.ui.theme.ShivaPdfTheme

import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pre-create WebView cache directories to prevent Chromium simple_index_file write failure
        try {
            val baseCache = File(cacheDir, "WebView/Default/HTTP Cache/index-dir")
            if (!baseCache.exists()) baseCache.mkdirs()
            val codeCache = File(cacheDir, "WebView/Default/Code Cache/js")
            if (!codeCache.exists()) codeCache.mkdirs()
            val wasmCache = File(cacheDir, "WebView/Default/Code Cache/wasm")
            if (!wasmCache.exists()) wasmCache.mkdirs()
        } catch (_: Exception) {}

        enableEdgeToEdge()
        setContent {
            ShivaPdfTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ShivaBackground
                ) {
                    ShivaPdfApp()
                }
            }
        }
    }
}

@Composable
fun ShivaPdfApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activePdf by viewModel.activePdf.collectAsState()
    val downloadedPdfs by viewModel.downloadedPdfs.collectAsState()

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            AppScreen.SPLASH -> {
                SplashScreen(
                    onSplashFinished = { viewModel.finishSplash() }
                )
            }

            AppScreen.MAIN -> {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenDownloads = { viewModel.openDownloads() }
                )
            }

            AppScreen.DOWNLOADS -> {
                DownloadsScreen(
                    downloadedList = downloadedPdfs,
                    onOpenPdf = { item ->
                        item.localFilePath?.let { path ->
                            viewModel.openPdfReader(item.title, path, item)
                        }
                    },
                    onDeletePdf = { item ->
                        viewModel.deletePdf(item)
                    },
                    onToggleFavorite = { item ->
                        viewModel.toggleFavorite(item)
                    },
                    onBack = { viewModel.closeDownloads() },
                    onBrowseLibrary = { viewModel.closeDownloads() }
                )
            }

            AppScreen.PDF_READER -> {
                activePdf?.let { pdf ->
                    PdfReaderScreen(
                        pdfTitle = pdf.title,
                        filePath = pdf.filePath,
                        originalItem = pdf.originalItem,
                        onBack = { viewModel.closePdfReader() },
                        onToggleBookmark = { item ->
                            viewModel.toggleFavorite(item)
                        },
                        onUpdateProgress = { id, page ->
                            viewModel.updateReadingProgress(id, page)
                        }
                    )
                } ?: run {
                    viewModel.closePdfReader()
                }
            }
        }
    }
}
