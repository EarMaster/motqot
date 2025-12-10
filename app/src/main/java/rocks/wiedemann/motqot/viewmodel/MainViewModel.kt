package rocks.wiedemann.motqot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rocks.wiedemann.motqot.model.Quote
import rocks.wiedemann.motqot.repository.QuoteRepository
import rocks.wiedemann.motqot.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ViewModel for the main activity
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = QuoteRepository(application)
    
    private val _quote = MutableLiveData<Quote?>()
    val quote: LiveData<Quote?> = _quote
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    init {
        loadLastQuote()
    }
    
    /**
     * Load the last saved quote
     */
    fun loadLastQuote() {
        val lastQuote = repository.getLastQuote()
        _quote.value = lastQuote
    }
    
    /**
     * Generate a new quote
     */
    fun generateQuote() {
        if (_isLoading.value == true) return
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Get the preferred language from repository
                val language = repository.getLanguagePreference()
                
                val result = repository.generateQuote(language)
                if (result.isSuccess) {
                    _quote.value = result.getOrNull()
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
    fun formatDateForDisplay(date: Date): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Check if the provider configuration is ready
     */
    fun isApiConfigured(): Boolean {
        return repository.hasCompleteApiConfig()
    }
}
