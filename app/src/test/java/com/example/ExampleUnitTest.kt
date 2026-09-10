package com.example

import com.example.data.model.ShortenedUrlEntity
import com.example.data.remote.UrlShortenerService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun test_charsSavedAndPercentage() {
    val entity = ShortenedUrlEntity(
      originalUrl = "https://example.com/very/long/path/with/parameters?tracking=123456",
      shortUrl = "https://tinyurl.com/xyz12",
      provider = "TinyURL",
      originalLength = 65,
      shortLength = 25
    )

    assertEquals(40, entity.charsSaved)
    assertEquals(61, entity.percentSaved)
  }

  @Test
  fun test_localShortUrlGeneration() {
    val service = UrlShortenerService()
    val shortUrlWithAlias = service.generateLocalShortUrl("https://example.com", "meu-link")
    assertEquals("https://encurt.ar/meu-link", shortUrlWithAlias)

    val shortUrlAuto = service.generateLocalShortUrl("https://example.com/test", null)
    assertTrue(shortUrlAuto.startsWith("https://encurt.ar/"))
    assertEquals(24, shortUrlAuto.length)
  }
}
