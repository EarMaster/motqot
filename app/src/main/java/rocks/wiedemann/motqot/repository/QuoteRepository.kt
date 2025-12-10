package rocks.wiedemann.motqot.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import rocks.wiedemann.motqot.MotQotApplication
import rocks.wiedemann.motqot.R
import rocks.wiedemann.motqot.api.ApiProviderConfig
import rocks.wiedemann.motqot.api.OpenAiCompatibleClient
import rocks.wiedemann.motqot.model.Quote
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository for managing quote data
 */
class QuoteRepository(private val context: Context) {
    private val TAG = "QuoteRepository"
    
    private val apiClient = OpenAiCompatibleClient()
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
     * Get the user's preferred language for quotes
     */
    fun getLanguagePreference(): String {
        return sharedPreferences.getString(
            "language_preference",  // Make sure this matches your preferences.xml key
            "en"  // Default to English
        ) ?: "en"
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
    
    fun hasCompleteApiConfig(): Boolean = buildApiConfig() != null

    /**
     * Generate a new quote using the configured provider
     */
    suspend fun generateQuote(language: String = "en"): Result<Quote> {
        val config = buildApiConfig()
            ?: return Result.failure(Exception(context.getString(R.string.error_incomplete_api_config)))

        return try {
            apiClient.generateMotivationalQuote(config, language)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildApiConfig(): ApiProviderConfig? {
        val apiKey = getApiKey()
        val baseUrl = sharedPreferences.getString(MotQotApplication.KEY_API_BASE_URL, null)
        val model = sharedPreferences.getString(MotQotApplication.KEY_API_MODEL, null)

        if (apiKey.isNullOrBlank() || baseUrl.isNullOrBlank() || model.isNullOrBlank()) {
            return null
        }

        return ApiProviderConfig(
            baseUrl = baseUrl,
            apiKey = apiKey,
            model = model
        )
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
