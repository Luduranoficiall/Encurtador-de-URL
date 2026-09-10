package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ShortenedUrlEntity
import com.example.data.remote.ShortenerProvider
import com.example.ui.components.QrCodeView
import com.example.ui.components.UrlInputSection
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.CyanPrimaryLight
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlShortenerScreen(
  viewModel: UrlShortenerViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val historyUrls by viewModel.historyUrls.collectAsStateWithLifecycle()
  val stats by viewModel.stats.collectAsStateWithLifecycle()

  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val focusManager = LocalFocusManager.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }

  fun copyToClipboard(text: String, label: String = "Link Encurtado") {
    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
    Toast.makeText(context, "Link copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
    scope.launch {
      snackbarHostState.showSnackbar("Copiado: $text")
    }
  }

  fun shareUrl(url: String) {
    val sendIntent: Intent = Intent().apply {
      action = Intent.ACTION_SEND
      putExtra(Intent.EXTRA_TEXT, url)
      type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Link")
    context.startActivity(shareIntent)
  }

  fun openInBrowser(url: String, id: Long? = null) {
    try {
      val clean = if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://$url"
      } else url
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(clean))
      context.startActivity(intent)
      id?.let { viewModel.recordClick(it) }
    } catch (e: Exception) {
      Toast.makeText(context, "Não foi possível abrir o link: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun pasteFromClipboard() {
    val systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val clip = systemClipboard?.primaryClip
    if (clip != null && clip.itemCount > 0) {
      val pasted = clip.getItemAt(0).coerceToText(context).toString().trim()
      if (pasted.isNotEmpty()) {
        viewModel.onUrlChanged(pasted)
        Toast.makeText(context, "Link colado da área de transferência", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, "Área de transferência vazia", Toast.LENGTH_SHORT).show()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface,
          titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                  Brush.linearGradient(listOf(CyanPrimary, EmeraldSuccess))
                ),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Encurtador de URL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
              )
              Text(
                text = "Links rápidos, QR Code & Histórico",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          if (historyUrls.isNotEmpty()) {
            IconButton(
              onClick = { viewModel.setShowClearConfirm(true) },
              modifier = Modifier.testTag("clear_history_button")
            ) {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Limpar histórico",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      )
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Stats Banner
      item(key = "stats_banner") {
        StatsHeader(stats = stats)
      }

      // 2. Input Component (URL Text Field + Shorten Button)
      item(key = "input_card") {
        Column(modifier = Modifier.fillMaxWidth()) {
          UrlInputSection(
            url = uiState.urlInput,
            onUrlChange = { viewModel.onUrlChanged(it) },
            onShortenClick = { viewModel.shortenUrl() },
            isLoading = uiState.isLoading,
            onPasteClick = { pasteFromClipboard() },
            selectedProvider = uiState.selectedProvider,
            onProviderSelected = { viewModel.onProviderSelected(it) },
            alias = uiState.aliasInput,
            onAliasChange = { viewModel.onAliasChanged(it) }
          )

          // Error display
          AnimatedVisibility(visible = uiState.errorMessage != null) {
            uiState.errorMessage?.let { errorMsg ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 10.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  Icons.Outlined.Info,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = errorMsg,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.weight(1f)
                )
                IconButton(
                  onClick = { viewModel.dismissError() },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    Icons.Default.Close,
                    contentDescription = "Fechar erro",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }
            }
          }
        }
      }

      // 3. Highlight Card for Just-Shortened Result
      uiState.lastShortened?.let { result ->
        item(key = "last_result_${result.id}") {
          ResultCard(
            item = result,
            onCopy = { copyToClipboard(result.shortUrl) },
            onShare = { shareUrl(result.shortUrl) },
            onOpen = { openInBrowser(result.shortUrl, result.id) },
            onShowQr = { viewModel.showQr(result.shortUrl) }
          )
        }
      }

      // 4. History Header & Search
      item(key = "history_header") {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Histórico de Links",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            if (historyUrls.isNotEmpty()) {
              Text(
                text = "${historyUrls.size} ${if (historyUrls.size == 1) "link" else "links"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          if (historyUrls.isNotEmpty() || uiState.searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
              value = uiState.searchQuery,
              onValueChange = { viewModel.onSearchQueryChanged(it) },
              placeholder = { Text("Pesquisar no histórico...") },
              leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
              },
              trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                  IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Limpar pesquisa")
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
              )
            )
          }
        }
      }

      // 5. History List or Empty State
      if (historyUrls.isEmpty()) {
        item(key = "empty_history") {
          EmptyHistoryState(isSearching = uiState.searchQuery.isNotEmpty())
        }
      } else {
        items(
          items = historyUrls,
          key = { it.id }
        ) { urlItem ->
          HistoryUrlCard(
            item = urlItem,
            onCopy = { copyToClipboard(urlItem.shortUrl) },
            onShare = { shareUrl(urlItem.shortUrl) },
            onOpen = { openInBrowser(urlItem.shortUrl, urlItem.id) },
            onShowQr = { viewModel.showQr(urlItem.shortUrl) },
            onDelete = { viewModel.deleteUrl(urlItem.id) }
          )
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // QR Code Dialog
  uiState.qrDialogUrl?.let { qrUrl ->
    Dialog(onDismissRequest = { viewModel.dismissQr() }) {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp)
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "QR Code do Link",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            IconButton(
              onClick = { viewModel.dismissQr() },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Fechar")
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          QrCodeView(
            data = qrUrl,
            size = 200.dp
          )

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = qrUrl,
            style = MaterialTheme.typography.bodyMedium.copy(
              fontWeight = FontWeight.SemiBold,
              fontFamily = FontFamily.Monospace
            ),
            color = CyanPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(20.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedButton(
              onClick = { copyToClipboard(qrUrl) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Copiar")
            }

            Button(
              onClick = { shareUrl(qrUrl) },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
              Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Compartilhar")
            }
          }
        }
      }
    }
  }

  // Clear All Confirmation Dialog
  if (uiState.showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { viewModel.setShowClearConfirm(false) },
      title = { Text("Limpar histórico?") },
      text = { Text("Isso removerá todos os links encurtados salvos localmente no seu dispositivo.") },
      confirmButton = {
        Button(
          onClick = { viewModel.clearHistory() },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Limpar tudo")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.setShowClearConfirm(false) }) {
          Text("Cancelar")
        }
      }
    )
  }
}

@Composable
fun StatsHeader(
  stats: ShortenerStats,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    StatPill(
      title = "Encurtados",
      value = "${stats.totalShortened}",
      icon = Icons.Default.Link,
      accentColor = CyanPrimary,
      modifier = Modifier.weight(1f)
    )
    StatPill(
      title = "Chars Salvos",
      value = "${stats.totalCharsSaved}",
      icon = Icons.Default.TrendingDown,
      accentColor = EmeraldSuccess,
      modifier = Modifier.weight(1.2f)
    )
    StatPill(
      title = "Acessos",
      value = "${stats.totalClicks}",
      icon = Icons.Default.OpenInBrowser,
      accentColor = CyanPrimaryLight,
      modifier = Modifier.weight(0.9f)
    )
  }
}

@Composable
fun StatPill(
  title: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier
        .padding(vertical = 12.dp, horizontal = 10.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(30.dp)
          .clip(CircleShape)
          .background(accentColor.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = accentColor,
          modifier = Modifier.size(16.dp)
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
      Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun ResultCard(
  item: ShortenedUrlEntity,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  onOpen: () -> Unit,
  onShowQr: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = 1.5.dp,
        brush = Brush.horizontalGradient(listOf(CyanPrimary, EmeraldSuccess)),
        shape = RoundedCornerShape(20.dp)
      )
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = EmeraldSuccess,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Link Encurtado com Sucesso!",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = EmeraldSuccess
          )
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = EmeraldSuccess.copy(alpha = 0.15f)
        ) {
          Text(
            text = "-${item.percentSaved}% (${item.charsSaved} chars)",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = EmeraldSuccess,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Short URL Display
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = item.shortUrl,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
              ),
              color = CyanPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
            Text(
              text = item.originalUrl,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Quick Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onCopy,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("result_copy_button"),
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Copiar")
        }

        OutlinedButton(
          onClick = onShare,
          modifier = Modifier
            .weight(1f)
            .height(44.dp)
            .testTag("result_share_button"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Enviar")
        }

        IconButton(
          onClick = onShowQr,
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .testTag("result_qr_button")
        ) {
          Icon(Icons.Default.QrCode2, contentDescription = "Ver QR Code", tint = CyanPrimary)
        }

        IconButton(
          onClick = onOpen,
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .testTag("result_open_button")
        ) {
          Icon(Icons.Default.OpenInBrowser, contentDescription = "Abrir Link", tint = MaterialTheme.colorScheme.onSurface)
        }
      }
    }
  }
}

@Composable
fun HistoryUrlCard(
  item: ShortenedUrlEntity,
  onCopy: () -> Unit,
  onShare: () -> Unit,
  onOpen: () -> Unit,
  onShowQr: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier
) {
  val dateFormatted = remember(item.createdAt) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    sdf.format(Date(item.createdAt))
  }

  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = item.shortUrl,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
          ),
          color = CyanPrimary,
          modifier = Modifier.weight(1f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = item.provider,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Spacer(modifier = Modifier.width(6.dp))

          IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              Icons.Default.Delete,
              contentDescription = "Excluir link",
              tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = item.originalUrl,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(8.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = dateFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (item.clicks > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "• ${item.clicks} ${if (item.clicks == 1) "clique" else "cliques"}",
              style = MaterialTheme.typography.labelSmall,
              color = EmeraldSuccess
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          IconButton(onClick = onCopy, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = onShare, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.Share, contentDescription = "Compartilhar", modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = onShowQr, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.QrCode2, contentDescription = "Ver QR", modifier = Modifier.size(16.dp))
          }
          IconButton(onClick = onOpen, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.OpenInBrowser, contentDescription = "Abrir", modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}

@Composable
fun EmptyHistoryState(isSearching: Boolean) {
  Card(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    ),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(CyanPrimary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isSearching) Icons.Default.Search else Icons.Default.Link,
          contentDescription = null,
          tint = CyanPrimary,
          modifier = Modifier.size(28.dp)
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = if (isSearching) "Nenhum link encontrado" else "Nenhum link encurtado ainda",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = if (isSearching)
          "Tente buscar por outras palavras-chave ou limpe a busca."
        else
          "Cole uma URL longa no campo acima para gerar seu primeiro link reduzido e QR Code.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )
    }
  }
}
