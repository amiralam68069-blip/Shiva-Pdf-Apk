package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SavedPdfItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPdfDao {
    @Query("SELECT * FROM saved_pdfs ORDER BY downloadTimestamp DESC")
    fun getAllSavedPdfs(): Flow<List<SavedPdfItem>>

    @Query("SELECT * FROM saved_pdfs WHERE isDownloaded = 1 ORDER BY downloadTimestamp DESC")
    fun getDownloadedPdfs(): Flow<List<SavedPdfItem>>

    @Query("SELECT * FROM saved_pdfs WHERE isFavorite = 1 ORDER BY downloadTimestamp DESC")
    fun getFavoritePdfs(): Flow<List<SavedPdfItem>>

    @Query("SELECT * FROM saved_pdfs WHERE originalUrl = :url LIMIT 1")
    suspend fun getByUrl(url: String): SavedPdfItem?

    @Query("SELECT * FROM saved_pdfs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SavedPdfItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SavedPdfItem): Long

    @Update
    suspend fun update(item: SavedPdfItem)

    @Query("DELETE FROM saved_pdfs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE saved_pdfs SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE saved_pdfs SET lastPageRead = :page, lastReadTimestamp = :time WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, time: Long)
}
