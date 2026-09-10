package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ShortenedUrlEntity
import com.example.data.remote.ShortenerProvider
import com.example.data.repository.UrlRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShortenerUiState(
  val urlInput: String = "",
  val aliasInput: String = "",
  val selectedProvider: ShortenerProvider = ShortenerProvider.TINY_URL,
  val isLoading: Boolean = false,
  val errorMessage: String? = null,
  val lastShortened: ShortenedUrlEntity? = null,
  val searchQuery: String = "",
  val qrDialogUrl: String? = null,
  val showClearConfirmDialog: Boolean = false
)

data class ShortenerStats(
  val totalShortened: Int = 0,
  val totalCharsSaved: Int = 0,
  val totalClicks: Int = 0
)

class UrlShortenerViewModel(
  private val repository: UrlRepository
) : ViewModel() {

  private val _uiState = MutableStateFlow(ShortenerUiState())
  val uiState: StateFlow<ShortenerUiState> = _uiState.asStateFlow()

  @OptIn(ExperimentalCoroutinesApi::class)
  val historyUrls: StateFlow<List<ShortenedUrlEntity>> = _uiState
    .flatMapLatest { state ->
      if (state.searchQuery.isBlank()) {
        repository.allUrls
      } else {
        repository.search(state.searchQuery.trim())
      }
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  val stats: StateFlow<ShortenerStats> = repository.allUrls
    .combine(_uiState) { list, _ ->
      ShortenerStats(
        totalShortened = list.size,
        totalCharsSaved = list.sumOf { it.charsSaved },
        totalClicks = list.sumOf { it.clicks }
      )
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = ShortenerStats()
    )

  fun onUrlChanged(newUrl: String) {
    _uiState.value = _uiState.value.copy(urlInput = newUrl, errorMessage = null)
  }

  fun onAliasChanged(newAlias: String) {
    _uiState.value = _uiState.value.copy(aliasInput = newAlias)
  }

  fun onProviderSelected(provider: ShortenerProvider) {
    _uiState.value = _uiState.value.copy(selectedProvider = provider)
  }

  fun onSearchQueryChanged(query: String) {
    _uiState.value = _uiState.value.copy(searchQuery = query)
  }

  fun shortenUrl() {
    val currentState = _uiState.value
    val url = currentState.urlInput.trim()
    if (url.isEmpty()) {
      _uiState.value = currentState.copy(errorMessage = "Insira um link para encurtar.")
      return
    }

    _uiState.value = currentState.copy(isLoading = true, errorMessage = null)

    viewModelScope.launch {
      val result = repository.shortenAndSave(
        url = url,
        provider = currentState.selectedProvider,
        customAlias = currentState.aliasInput.takeIf { it.isNotBlank() }
      )

      result.fold(
        onSuccess = { entity ->
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            lastShortened = entity,
            urlInput = "",
            aliasInput = "",
            errorMessage = null
          )
        },
        onFailure = { throwable ->
          _uiState.value = _uiState.value.copy(
            isLoading = false,
            errorMessage = throwable.localizedMessage ?: "Ocorreu um erro ao encurtar o link."
          )
        }
      )
    }
  }

  fun setSharedUrl(sharedText: String) {
    // Extract url if surrounded by text
    val urlRegex = """https?://[^\s]+""".toRegex()
    val match = urlRegex.find(sharedText)
    val extracted = match?.value ?: sharedText.trim()
    _uiState.value = _uiState.value.copy(urlInput = extracted, errorMessage = null)
  }

  fun deleteUrl(id: Long) {
    viewModelScope.launch {
      repository.deleteUrl(id)
      if (_uiState.value.lastShortened?.id == id) {
        _uiState.value = _uiState.value.copy(lastShortened = null)
      }
    }
  }

  fun clearHistory() {
    viewModelScope.launch {
      repository.clearHistory()
      _uiState.value = _uiState.value.copy(
        lastShortened = null,
        showClearConfirmDialog = false
      )
    }
  }

  fun setShowClearConfirm(show: Boolean) {
    _uiState.value = _uiState.value.copy(showClearConfirmDialog = show)
  }

  fun recordClick(id: Long) {
    viewModelScope.launch {
      repository.recordClick(id)
    }
  }

  fun showQr(url: String) {
    _uiState.value = _uiState.value.copy(qrDialogUrl = url)
  }

  fun dismissQr() {
    _uiState.value = _uiState.value.copy(qrDialogUrl = null)
  }

  fun dismissError() {
    _uiState.value = _uiState.value.copy(errorMessage = null)
  }

  class Factory(private val repository: UrlRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass.isAssignableFrom(UrlShortenerViewModel::class.java)) {
        return UrlShortenerViewModel(repository) as T
      }
      throw IllegalArgumentException("Unknown ViewModel class")
    }
  }
}
