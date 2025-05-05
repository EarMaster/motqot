package rocks.wiedemann.motqot.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rocks.wiedemann.motqot.model.Quote
import java.util.Locale

/**
 * Client for interacting with the Perplexity API
 */
class PerplexityApiClient {
    private val TAG = "PerplexityApiClient"
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(PerplexityApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    
    private val apiService = retrofit.create(PerplexityApiService::class.java)
    
    /**
     * Generate a motivational quote using the Perplexity API
     * 
     * @param apiKey The Perplexity API key
     * @param language The language code (e.g., "en", "de", "fr", "es")
     * @return A Quote object containing the generated quote
     */
    suspend fun generateMotivationalQuote(apiKey: String, language: String): Result<Quote> {
        return withContext(Dispatchers.IO) {
            try {
                val languageName = when (language) {
                    "de" -> "German"
                    "fr" -> "French"
                    "es" -> "Spanish"
                    else -> "English"
                }
                
                val prompt = "Generate a short, inspiring motivational quote for programmers or developers. " +
                        "The quote should be in $languageName. " +
                        "Return ONLY the quote text without any additional information, attribution, or explanation."
                
                val requestJson = JSONObject().apply {
                    put("model", "mistral-7b-instruct")
                    put("max_tokens", 100)
                    put("temperature", 0.7)
                    put("messages", listOf(
                        mapOf(
                            "role" to "system",
                            "content" to "You are a motivational quote generator for programmers and developers."
                        ),
                        mapOf(
                            "role" to "user",
                            "content" to prompt
                        )
                    ))
                }
                
                val requestBody = requestJson.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())
                
                val response = apiService.generateQuote("Bearer $apiKey", requestBody)
                
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val firstChoice = choices.getJSONObject(0)
                        val message = firstChoice.getJSONObject("message")
                        val content = message.getString("content").trim()
                        
                        // Remove quotes if present
                        val cleanedContent = content.replace("^[\"']|[\"']$".toRegex(), "")
                        
                        Result.success(Quote(cleanedContent, language = language))
                    } else {
                        Result.failure(Exception("No quote generated"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "API Error: $errorBody")
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during API call", e)
                Result.failure(e)
            }
        }
    }
}
