package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.remote.ShortenerProvider
import com.example.ui.theme.CyanPrimary

/**
 * Reusable Compose UI component featuring:
 * 1. An input text field for the target long URL with paste/clear actions
 * 2. Optional provider selection and custom alias
 * 3. A prominent "Shorten" button to trigger the shortening logic
 */
@Composable
fun UrlInputSection(
  url: String,
  onUrlChange: (String) -> Unit,
  onShortenClick: () -> Unit,
  isLoading: Boolean,
  modifier: Modifier = Modifier,
  onPasteClick: (() -> Unit)? = null,
  selectedProvider: ShortenerProvider = ShortenerProvider.TINY_URL,
  onProviderSelected: ((ShortenerProvider) -> Unit)? = null,
  alias: String = "",
  onAliasChange: ((String) -> Unit)? = null,
  enableAdvancedOptions: Boolean = true
) {
  val focusManager = LocalFocusManager.current
  var showAdvanced by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Text(
        text = "Cole o link longo",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
      )
      Text(
        text = "Diminua os caracteres para compartilhar facilmente",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp)
      )

      // Text Field to input the URL
      OutlinedTextField(
        value = url,
        onValueChange = onUrlChange,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("url_input_field"),
        placeholder = { Text("https://exemplo.com/link-muito-longo...") },
        leadingIcon = {
          Icon(Icons.Default.Link, contentDescription = null, tint = CyanPrimary)
        },
        trailingIcon = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (url.isNotEmpty()) {
              IconButton(onClick = { onUrlChange("") }) {
                Icon(Icons.Default.Clear, contentDescription = "Limpar link")
              }
            } else if (onPasteClick != null) {
              IconButton(
                onClick = onPasteClick,
                modifier = Modifier.testTag("paste_button")
              ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Colar link")
              }
            }
          }
        },
        singleLine = false,
        maxLines = 3,
        shape = RoundedCornerShape(14.dp),
        keyboardOptions = KeyboardOptions(
          keyboardType = KeyboardType.Uri,
          imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
          onDone = {
            focusManager.clearFocus()
            onShortenClick()
          }
        ),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CyanPrimary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
        )
      )

      if (enableAdvancedOptions && onProviderSelected != null) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvanced = !showAdvanced }
            .padding(vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "Opções avançadas (Provedor & Apelido)",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = CyanPrimary
          )
          Icon(
            imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = CyanPrimary
          )
        }

        AnimatedVisibility(visible = showAdvanced) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp, bottom = 12.dp)
          ) {
            Text(
              text = "Provedor de Encurtamento",
              style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              ShortenerProvider.values().forEach { provider ->
                FilterChip(
                  selected = selectedProvider == provider,
                  onClick = { onProviderSelected(provider) },
                  label = { Text(provider.displayName) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CyanPrimary.copy(alpha = 0.15f),
                    selectedLabelColor = CyanPrimary
                  )
                )
              }
            }

            if (selectedProvider.supportsAlias && onAliasChange != null) {
              Spacer(modifier = Modifier.height(10.dp))
              OutlinedTextField(
                value = alias,
                onValueChange = onAliasChange,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("alias_input_field"),
                placeholder = { Text("Apelido personalizado (opcional)") },
                label = { Text("Apelido / Custom Slug") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = CyanPrimary,
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Button to trigger the shortening logic
      Button(
        onClick = {
          focusManager.clearFocus()
          onShortenClick()
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("shorten_button"),
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = Color.White,
            strokeWidth = 2.5.dp
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text("Encurtando...", style = MaterialTheme.typography.bodyMedium)
        } else {
          Icon(Icons.Default.Link, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Encurtar URL",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}
