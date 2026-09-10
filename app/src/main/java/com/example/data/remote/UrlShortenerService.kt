package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

enum class ShortenerProvider(val displayName: String, val supportsAlias: Boolean) {
  TINY_URL("TinyURL", true),
  IS_GD("is.gd", true),
  CLEAN_URI("CleanURI", false),
  LOCAL("Local / Offline", true)
}

class UrlShortenerService {

  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  suspend fun shorten(
    url: String,
    provider: ShortenerProvider,
    customAlias: String? = null
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      val sanitizedUrl = sanitizeUrl(url)
      when (provider) {
        ShortenerProvider.TINY_URL -> shortenWithTinyUrl(sanitizedUrl, customAlias)
        ShortenerProvider.IS_GD -> shortenWithIsGd(sanitizedUrl, customAlias)
        ShortenerProvider.CLEAN_URI -> shortenWithCleanUri(sanitizedUrl)
        ShortenerProvider.LOCAL -> Result.success(generateLocalShortUrl(sanitizedUrl, customAlias))
      }
    } catch (e: Exception) {
      // Fallback: If network fails or server is unreachable, offer local shortened link or error
      Result.failure(e)
    }
  }

  private fun sanitizeUrl(input: String): String {
    val trimmed = input.trim()
    return if (!trimmed.startsWith("http://", ignoreCase = true) &&
      !trimmed.startsWith("https://", ignoreCase = true)
    ) {
      "https://$trimmed"
    } else {
      trimmed
    }
  }

  private fun shortenWithTinyUrl(url: String, alias: String?): Result<String> {
    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    var endpoint = "https://tinyurl.com/api-create.php?url=$encodedUrl"
    if (!alias.isNullOrBlank()) {
      val encodedAlias = URLEncoder.encode(alias.trim(), StandardCharsets.UTF_8.toString())
      endpoint += "&alias=$encodedAlias"
    }

    val request = Request.Builder()
      .url(endpoint)
      .header("User-Agent", "Mozilla/5.0 (Android; URLShortenerApp)")
      .get()
      .build()

    client.newCall(request).execute().use { response ->
      val body = response.body?.string()?.trim().orEmpty()
      if (response.isSuccessful && body.startsWith("http")) {
        return Result.success(body)
      } else {
        val errorMessage = if (body.contains("Error", ignoreCase = true)) {
          body
        } else if (response.code == 422 || !alias.isNullOrBlank()) {
          "O apelido personalizado '$alias' já está em uso ou é inválido."
        } else {
          "Falha ao encurtar pelo TinyURL (Código ${response.code})."
        }
        return Result.failure(Exception(errorMessage))
      }
    }
  }

  private fun shortenWithIsGd(url: String, alias: String?): Result<String> {
    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    var endpoint = "https://is.gd/create.php?format=simple&url=$encodedUrl"
    if (!alias.isNullOrBlank()) {
      val encodedAlias = URLEncoder.encode(alias.trim(), StandardCharsets.UTF_8.toString())
      endpoint += "&shorturl=$encodedAlias"
    }

    val request = Request.Builder()
      .url(endpoint)
      .header("User-Agent", "Mozilla/5.0 (Android; URLShortenerApp)")
      .get()
      .build()

    client.newCall(request).execute().use { response ->
      val body = response.body?.string()?.trim().orEmpty()
      if (response.isSuccessful && body.startsWith("http")) {
        return Result.success(body)
      } else {
        val message = if (body.contains("Error:", ignoreCase = true)) {
          body
        } else {
          "Falha ao encurtar com is.gd (${response.code})."
        }
        return Result.failure(Exception(message))
      }
    }
  }

  private fun shortenWithCleanUri(url: String): Result<String> {
    val requestBody = FormBody.Builder()
      .add("url", url)
      .build()

    val request = Request.Builder()
      .url("https://cleanuri.com/api/v1/shorten")
      .post(requestBody)
      .build()

    client.newCall(request).execute().use { response ->
      val body = response.body?.string()?.trim().orEmpty()
      if (response.isSuccessful && body.isNotEmpty()) {
        val json = JSONObject(body)
        if (json.has("result_url")) {
          return Result.success(json.getString("result_url"))
        } else if (json.has("error")) {
          return Result.failure(Exception(json.getString("error")))
        }
      }
      return Result.failure(Exception("Falha ao comunicar com CleanURI (${response.code})."))
    }
  }

  fun generateLocalShortUrl(url: String, alias: String?): String {
    if (!alias.isNullOrBlank()) {
      val cleanAlias = alias.trim().replace("\\s+".toRegex(), "-")
      return "https://encurt.ar/$cleanAlias"
    }
    // Generate 6-char base62-like hash from MD5
    val md5 = MessageDigest.getInstance("MD5").digest(url.toByteArray(StandardCharsets.UTF_8))
    val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    val code = StringBuilder()
    for (i in 0 until 6) {
      val byteVal = (md5[i].toInt() and 0xFF)
      code.append(chars[byteVal % chars.length])
    }
    return "https://encurt.ar/$code"
  }
}
