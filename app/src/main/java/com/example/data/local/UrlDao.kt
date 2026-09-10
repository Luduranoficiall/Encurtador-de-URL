package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ShortenedUrlEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UrlDao {

  @Query("SELECT * FROM shortened_urls ORDER BY createdAt DESC")
  fun getAllUrls(): Flow<List<ShortenedUrlEntity>>

  @Query("SELECT * FROM shortened_urls WHERE originalUrl LIKE '%' || :query || '%' OR shortUrl LIKE '%' || :query || '%' ORDER BY createdAt DESC")
  fun searchUrls(query: String): Flow<List<ShortenedUrlEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertUrl(urlEntity: ShortenedUrlEntity): Long

  @Update
  suspend fun updateUrl(urlEntity: ShortenedUrlEntity)

  @Query("DELETE FROM shortened_urls WHERE id = :id")
  suspend fun deleteUrlById(id: Long)

  @Query("DELETE FROM shortened_urls")
  suspend fun clearAll()

  @Query("UPDATE shortened_urls SET clicks = clicks + 1 WHERE id = :id")
  suspend fun incrementClicks(id: Long)
}
