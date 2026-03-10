package rocks.wiedemann.motqot.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import rocks.wiedemann.motqot.MotQotApplication
import rocks.wiedemann.motqot.R
import rocks.wiedemann.motqot.api.ApiProviderConfig
import rocks.wiedemann.motqot.api.OpenAiCompatibleClient
import rocks.wiedemann.motqot.model.Quote
import java.time.LocalDate

/**
 * Repository for managing quote data
 */
class QuoteRepository(private val context: Context) {
    private val TAG = "QuoteRepository"

    private val apiClient = OpenAiCompatibleClient()
    private val gson = GsonBuilder()
        .registerTypeAdapter(LocalDate::class.java, object : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
            override fun serialize(src: LocalDate, typeOfSrc: java.lang.reflect.Type, context: JsonSerializationContext) =
                JsonPrimitive(src.toString())
            override fun deserialize(json: JsonElement, typeOfT: java.lang.reflect.Type, context: JsonDeserializationContext): LocalDate =
                LocalDate.parse(json.asString)
        })
        .create()

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
            "language_preference",
            "en"
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
            .putString(MotQotApplication.KEY_LAST_QUOTE_DATE, quote.date.toString())
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
            ?: return true
        return LocalDate.now().toString() != lastQuoteDateStr
    }
}
