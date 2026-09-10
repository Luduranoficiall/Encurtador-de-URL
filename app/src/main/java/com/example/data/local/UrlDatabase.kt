package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ShortenedUrlEntity

@Database(entities = [ShortenedUrlEntity::class], version = 1, exportSchema = false)
abstract class UrlDatabase : RoomDatabase() {

  abstract fun urlDao(): UrlDao

  companion object {
    @Volatile
    private var INSTANCE: UrlDatabase? = null

    fun getDatabase(context: Context): UrlDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          UrlDatabase::class.java,
          "shortened_urls_db"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
