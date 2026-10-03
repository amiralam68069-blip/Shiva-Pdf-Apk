package com.example.data.repository

import android.content.Context
import android.os.Environment
import com.example.data.local.SavedPdfDao
import com.example.data.model.SavedPdfItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PdfRepository(
    private val savedPdfDao: SavedPdfDao,
    private val context: Context
) {
    val allSavedPdfs: Flow<List<SavedPdfItem>> = savedPdfDao.getAllSavedPdfs()
    val downloadedPdfs: Flow<List<SavedPdfItem>> = savedPdfDao.getDownloadedPdfs()
    val favoritePdfs: Flow<List<SavedPdfItem>> = savedPdfDao.getFavoritePdfs()

    suspend fun getPdfById(id: Long): SavedPdfItem? = withContext(Dispatchers.IO) {
        savedPdfDao.getById(id)
    }

    suspend fun getPdfByUrl(url: String): SavedPdfItem? = withContext(Dispatchers.IO) {
        savedPdfDao.getByUrl(url)
    }

    suspend fun toggleFavorite(item: SavedPdfItem) = withContext(Dispatchers.IO) {
        savedPdfDao.updateFavorite(item.id, !item.isFavorite)
    }

    suspend fun updateReadingProgress(id: Long, page: Int) = withContext(Dispatchers.IO) {
        savedPdfDao.updateProgress(id, page, System.currentTimeMillis())
    }

    suspend fun deletePdf(item: SavedPdfItem) = withContext(Dispatchers.IO) {
        item.localFilePath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                file.delete()
            }
        }
        savedPdfDao.deleteById(item.id)
    }

    suspend fun downloadAndSavePdf(
        pdfUrl: String,
        title: String,
        category: String = "Notes",
        subject: String = "General",
        isPremium: Boolean = false,
        cookieHeader: String? = null,
        onProgress: (Int) -> Unit = {}
    ): Result<SavedPdfItem> = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val fileName = "ShivaPDF_${System.currentTimeMillis()}_$sanitizedName.pdf"
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            val destinationFile = File(downloadDir, fileName)

            val url = URL(pdfUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 30000
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                )
                if (!cookieHeader.isNullOrEmpty()) {
                    setRequestProperty("Cookie", cookieHeader)
                }
                instanceFollowRedirects = true
            }
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext Result.failure(Exception("Server returned HTTP $responseCode"))
            }

            val fileLength = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var total: Long = 0
                    var count: Int
                    while (input.read(buffer).also { count = it } != -1) {
                        total += count
                        if (fileLength > 0) {
                            val progress = ((total * 100) / fileLength).toInt()
                            onProgress(progress)
                        }
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            }

            val existing = savedPdfDao.getByUrl(pdfUrl)
            val itemToSave = (existing ?: SavedPdfItem(
                title = title,
                category = category,
                subject = subject,
                originalUrl = pdfUrl,
                isPremium = isPremium
            )).copy(
                localFilePath = destinationFile.absolutePath,
                fileSizeBytes = destinationFile.length(),
                isDownloaded = true,
                downloadTimestamp = System.currentTimeMillis()
            )

            val id = savedPdfDao.insert(itemToSave)
            Result.success(itemToSave.copy(id = if (existing != null) existing.id else id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
