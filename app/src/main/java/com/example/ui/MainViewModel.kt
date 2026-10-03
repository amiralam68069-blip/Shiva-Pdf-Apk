package com.example.ui

import android.app.Application
import android.webkit.CookieManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ShivaDatabase
import com.example.data.model.SavedPdfItem
import com.example.data.repository.PdfRepository
import com.example.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    MAIN,
    PDF_READER,
    DOWNLOADS
}

enum class NavigationTab(val label: String, val path: String) {
    HOME("Home", "/index.php"),
    PDFS("PDF Library", "/pdfs.php"),
    QUIZ("Quiz", "/quiz-list.php"),
    PURCHASES("My Purchases", "/my-purchases.php"),
    PROFILE("Profile", "/login.php")
}

data class ActivePdfReading(
    val title: String,
    val filePath: String,
    val originalItem: SavedPdfItem? = null
)

data class DownloadStatus(
    val isDownloading: Boolean = false,
    val title: String = "",
    val progress: Int = 0,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = ShivaDatabase.getDatabase(application)
    private val repository = PdfRepository(database.savedPdfDao(), application)
    private val networkMonitor = NetworkMonitor(application)

    private val _currentScreen = MutableStateFlow(AppScreen.SPLASH)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _currentTab = MutableStateFlow(NavigationTab.HOME)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _activePdf = MutableStateFlow<ActivePdfReading?>(null)
    val activePdf: StateFlow<ActivePdfReading?> = _activePdf.asStateFlow()

    private val _downloadStatus = MutableStateFlow(DownloadStatus())
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val _isWebLoading = MutableStateFlow(false)
    val isWebLoading: StateFlow<Boolean> = _isWebLoading.asStateFlow()

    private val _webProgress = MutableStateFlow(0)
    val webProgress: StateFlow<Int> = _webProgress.asStateFlow()

    private val _hasWebError = MutableStateFlow(false)
    val hasWebError: StateFlow<Boolean> = _hasWebError.asStateFlow()

    private val _webErrorMessage = MutableStateFlow("")
    val webErrorMessage: StateFlow<String> = _webErrorMessage.asStateFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), networkMonitor.isCurrentlyConnected())

    val downloadedPdfs: StateFlow<List<SavedPdfItem>> = repository.downloadedPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedPdfs: StateFlow<List<SavedPdfItem>> = repository.allSavedPdfs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun finishSplash() {
        _currentScreen.value = AppScreen.MAIN
    }

    fun selectTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun openDownloads() {
        _currentScreen.value = AppScreen.DOWNLOADS
    }

    fun closeDownloads() {
        _currentScreen.value = AppScreen.MAIN
    }

    fun openPdfReader(title: String, filePath: String, originalItem: SavedPdfItem? = null) {
        _activePdf.value = ActivePdfReading(title, filePath, originalItem)
        _currentScreen.value = AppScreen.PDF_READER
    }

    fun closePdfReader() {
        _activePdf.value = null
        _currentScreen.value = AppScreen.MAIN
    }

    fun setWebLoading(loading: Boolean, progress: Int = 0) {
        _isWebLoading.value = loading
        _webProgress.value = progress
        if (loading) {
            _hasWebError.value = false
        }
    }

    fun setWebError(errorMessage: String) {
        _hasWebError.value = true
        _webErrorMessage.value = errorMessage
        _isWebLoading.value = false
    }

    fun clearWebError() {
        _hasWebError.value = false
        _webErrorMessage.value = ""
    }

    fun downloadPdf(
        pdfUrl: String,
        title: String,
        isPremium: Boolean = false,
        openAfterDownload: Boolean = false
    ) {
        viewModelScope.launch {
            _downloadStatus.value = DownloadStatus(
                isDownloading = true,
                title = title,
                progress = 0
            )

            // Extract cookies from CookieManager to maintain session and bypass aes.js
            val cookie = try {
                CookieManager.getInstance().getCookie(pdfUrl)
            } catch (_: Exception) {
                null
            }

            val result = repository.downloadAndSavePdf(
                pdfUrl = pdfUrl,
                title = title,
                isPremium = isPremium,
                cookieHeader = cookie,
                onProgress = { prog ->
                    _downloadStatus.value = _downloadStatus.value.copy(progress = prog)
                }
            )

            result.onSuccess { savedItem ->
                _downloadStatus.value = DownloadStatus(isDownloading = false)
                if (openAfterDownload && savedItem.localFilePath != null) {
                    openPdfReader(savedItem.title, savedItem.localFilePath, savedItem)
                }
            }.onFailure { error ->
                _downloadStatus.value = DownloadStatus(
                    isDownloading = false,
                    errorMessage = error.localizedMessage ?: "Download failed"
                )
            }
        }
    }

    fun clearDownloadError() {
        _downloadStatus.value = _downloadStatus.value.copy(errorMessage = null)
    }

    fun deletePdf(item: SavedPdfItem) {
        viewModelScope.launch {
            repository.deletePdf(item)
        }
    }

    fun toggleFavorite(item: SavedPdfItem) {
        viewModelScope.launch {
            repository.toggleFavorite(item)
        }
    }

    fun updateReadingProgress(id: Long, page: Int) {
        viewModelScope.launch {
            repository.updateReadingProgress(id, page)
        }
    }
}
