package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_pdfs")
data class SavedPdfItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "General",
    val subject: String = "Study Material",
    val originalUrl: String,
    val localFilePath: String? = null,
    val fileSizeBytes: Long = 0L,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val isPremium: Boolean = false,
    val downloadTimestamp: Long = System.currentTimeMillis(),
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val totalPages: Int = 0,
    val lastPageRead: Int = 1
)
