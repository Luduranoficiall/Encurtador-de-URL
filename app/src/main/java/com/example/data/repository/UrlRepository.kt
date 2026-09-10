package com.example.data.repository

import com.example.data.local.UrlDao
import com.example.data.model.ShortenedUrlEntity
import com.example.data.remote.ShortenerProvider
import com.example.data.remote.UrlShortenerService
import kotlinx.coroutines.flow.Flow

class UrlRepository(
  private val urlDao: UrlDao,
  private val shortenerService: UrlShortenerService = UrlShortenerService()
) {

  val allUrls: Flow<List<ShortenedUrlEntity>> = urlDao.getAllUrls()

  fun search(query: String): Flow<List<ShortenedUrlEntity>> = urlDao.searchUrls(query)

  suspend fun shortenAndSave(
    url: String,
    provider: ShortenerProvider,
    customAlias: String?
  ): Result<ShortenedUrlEntity> {
    val cleanUrl = if (!url.startsWith("http://", true) && !url.startsWith("https://", true)) {
      "https://${url.trim()}"
    } else {
      url.trim()
    }

    val shortenResult = shortenerService.shorten(cleanUrl, provider, customAlias)
    return shortenResult.mapCatching { shortUrl ->
      val entity = ShortenedUrlEntity(
        originalUrl = cleanUrl,
        shortUrl = shortUrl,
        provider = provider.displayName,
        customAlias = customAlias?.takeIf { it.isNotBlank() },
        originalLength = cleanUrl.length,
        shortLength = shortUrl.length
      )
      val id = urlDao.insertUrl(entity)
      entity.copy(id = id)
    }
  }

  suspend fun deleteUrl(id: Long) = urlDao.deleteUrlById(id)

  suspend fun clearHistory() = urlDao.clearAll()

  suspend fun recordClick(id: Long) = urlDao.incrementClicks(id)
}
