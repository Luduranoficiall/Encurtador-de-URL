package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.ShortenedUrlEntity
import com.example.ui.ResultCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sample = ShortenedUrlEntity(
      id = 1,
      originalUrl = "https://www.google.com/search?q=encurtador+de+url+android+kotlin",
      shortUrl = "https://tinyurl.com/encurtador-app",
      provider = "TinyURL",
      originalLength = 65,
      shortLength = 33
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ResultCard(
          item = sample,
          onCopy = {},
          onShare = {},
          onOpen = {},
          onShowQr = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

