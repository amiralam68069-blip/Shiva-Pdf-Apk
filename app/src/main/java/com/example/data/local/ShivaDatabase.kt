package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.SavedPdfItem

@Database(entities = [SavedPdfItem::class], version = 1, exportSchema = false)
abstract class ShivaDatabase : RoomDatabase() {
    abstract fun savedPdfDao(): SavedPdfDao

    companion object {
        @Volatile
        private var INSTANCE: ShivaDatabase? = null

        fun getDatabase(context: Context): ShivaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShivaDatabase::class.java,
                    "shiva_pdf_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
