package rocks.wiedemann.motqot.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rocks.wiedemann.motqot.model.Quote
import rocks.wiedemann.motqot.repository.QuoteRepository
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
        
        // If there's no saved quote or we should generate a new one for today
        if (lastQuote == null || repository.shouldGenerateNewQuote()) {
            generateQuote()
        }
    }
    
    /**
     * Generate a new quote
     */
    fun generateQuote() {
        val apiKey = repository.getApiKey()
        if (apiKey.isNullOrBlank()) {
            _error.value = "API key not set"
            return
        }
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            repository.generateQuote()
                .onSuccess { quote ->
                    _quote.postValue(quote)
                    repository.saveQuote(quote)
                    _error.postValue(null)
                }
                .onFailure { exception ->
                    _error.postValue(exception.message)
                }
            
            _isLoading.postValue(false)
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
     * Check if the API key is set
     */
    fun isApiKeySet(): Boolean {
        return !repository.getApiKey().isNullOrBlank()
    }
}
