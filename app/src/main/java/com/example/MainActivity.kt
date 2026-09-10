package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.data.local.UrlDatabase
import com.example.data.repository.UrlRepository
import com.example.ui.UrlShortenerScreen
import com.example.ui.UrlShortenerViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

  private val viewModel: UrlShortenerViewModel by viewModels {
    val database = UrlDatabase.getDatabase(applicationContext)
    val repository = UrlRepository(database.urlDao())
    UrlShortenerViewModel.Factory(repository)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    handleIncomingIntent(intent)

    setContent {
      MyApplicationTheme {
        UrlShortenerScreen(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIncomingIntent(intent)
  }

  private fun handleIncomingIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
      intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
        viewModel.setSharedUrl(sharedText)
      }
    }
  }
}

