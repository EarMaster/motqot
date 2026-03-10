package rocks.wiedemann.motqot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rocks.wiedemann.motqot.model.Quote
import rocks.wiedemann.motqot.repository.QuoteRepository
import rocks.wiedemann.motqot.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ViewModel for the main activity
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = QuoteRepository(application)

    private val _quote = MutableStateFlow<Quote?>(null)
    val quote: StateFlow<Quote?> = _quote.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadLastQuote()
    }

    /**
     * Load the last saved quote
     */
    fun loadLastQuote() {
        _quote.value = repository.getLastQuote()
    }

    /**
     * Load the quote for today, or generate a new one if it's a new day.
     */
    fun loadQuoteForTodayOrGenerateNew() {
        if (repository.shouldGenerateNewQuote()) {
            generateQuote()
        } else {
            loadLastQuote()
        }
    }

    /**
     * Generate a new quote
     */
    fun generateQuote() {
        if (_isLoading.value) return

        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val language = repository.getLanguagePreference()
                val result = repository.generateQuote(language)
                if (result.isSuccess) {
                    val newQuote = result.getOrNull()
                    _quote.value = newQuote
                    if (newQuote != null) {
                        repository.saveQuote(newQuote)
                    }
                } else {
                    _error.value = result.exceptionOrNull()?.message
                        ?: getApplication<Application>().getString(R.string.error_generating_quote)
                }
            } catch (e: Exception) {
                _error.value = e.message
                    ?: getApplication<Application>().getString(R.string.error_generating_quote)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Format the date for display
     */
    fun formatDateForDisplay(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.getDefault())
        return date.format(formatter)
    }

    /**
     * Check if the provider configuration is ready
     */
    fun isApiConfigured(): Boolean {
        return repository.hasCompleteApiConfig()
    }
}
