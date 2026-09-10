package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shortened_urls")
data class ShortenedUrlEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val originalUrl: String,
  val shortUrl: String,
  val provider: String,
  val customAlias: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val originalLength: Int,
  val shortLength: Int,
  val clicks: Int = 0
) {
  val charsSaved: Int
    get() = (originalLength - shortLength).coerceAtLeast(0)

  val percentSaved: Int
    get() = if (originalLength > 0 && originalLength > shortLength) {
      (((originalLength - shortLength).toDouble() / originalLength) * 100).toInt()
    } else 0
}
