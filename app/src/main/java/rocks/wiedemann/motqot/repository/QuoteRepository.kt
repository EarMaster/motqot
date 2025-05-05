package rocks.wiedemann.motqot.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import rocks.wiedemann.motqot.MotQotApplication
import rocks.wiedemann.motqot.api.PerplexityApiClient
import rocks.wiedemann.motqot.model.Quote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for managing quote data
 */
class QuoteRepository(private val context: Context) {
    private val TAG = "QuoteRepository"
    
    private val apiClient = PerplexityApiClient()
    private val gson = Gson()
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        MotQotApplication.PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * Get the API key from SharedPreferences
     */
    fun getApiKey(): String? {
        return sharedPreferences.getString(MotQotApplication.KEY_API_KEY, null)
    }
    
    /**
     * Get the preferred language from SharedPreferences
     */
    fun getLanguage(): String {
        return sharedPreferences.getString(MotQotApplication.KEY_LANGUAGE, "en") ?: "en"
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(): Boolean {
        return sharedPreferences.getBoolean(MotQotApplication.KEY_ENABLE_NOTIFICATIONS, true)
    }
    
    /**
     * Get the last saved quote
     */
    fun getLastQuote(): Quote? {
        val quoteJson = sharedPreferences.getString(MotQotApplication.KEY_LAST_QUOTE, null)
        return if (quoteJson != null) {
            try {
                gson.fromJson(quoteJson, Quote::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing saved quote", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Save a quote to SharedPreferences
     */
    fun saveQuote(quote: Quote) {
        val quoteJson = gson.toJson(quote)
        sharedPreferences.edit()
            .putString(MotQotApplication.KEY_LAST_QUOTE, quoteJson)
            .putString(MotQotApplication.KEY_LAST_QUOTE_DATE, formatDate(quote.date))
            .apply()
    }
    
    /**
     * Generate a new quote using the Perplexity API
     */
    suspend fun generateQuote(): Result<Quote> {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return Result.failure(Exception("API key not set"))
        }
        
        val language = getLanguage()
        return apiClient.generateMotivationalQuote(apiKey, language)
    }
    
    /**
     * Check if a new quote should be generated today
     */
    fun shouldGenerateNewQuote(): Boolean {
        val lastQuoteDateStr = sharedPreferences.getString(MotQotApplication.KEY_LAST_QUOTE_DATE, null)
        if (lastQuoteDateStr == null) {
            return true
        }
        
        val today = formatDate(Date())
        return today != lastQuoteDateStr
    }
    
    /**
     * Format a date as yyyy-MM-dd
     */
    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
}
