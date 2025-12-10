package rocks.wiedemann.motqot.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import rocks.wiedemann.motqot.model.Quote

/**
 * Generic client for OpenAI-compatible chat completion APIs.
 */
class OpenAiCompatibleClient {

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    suspend fun generateMotivationalQuote(
        config: ApiProviderConfig,
        language: String
    ): Result<Quote> = withContext(Dispatchers.IO) {
        try {
            val service = createService(config.baseUrl)
            val prompt = buildPrompt(language)
            val requestJson = buildRequestJson(config.model, prompt)
            val requestBody = requestJson.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val response = service.createChatCompletion(
                authorization = "Bearer ${config.apiKey}",
                requestBody = requestBody
            )

            if (response.isSuccessful) {
                val responseBody = response.body()?.string().orEmpty()
                parseQuote(responseBody, language)
            } else {
                val errorBody = response.errorBody()?.string().orEmpty()
                Result.failure(Exception("API Error: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun createService(baseUrl: String): OpenAiApiService {
        val normalizedUrl = if (baseUrl.endsWith('/')) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAiApiService::class.java)
    }

    private fun buildPrompt(language: String): String {
        val languageName = when (language) {
            "de" -> "German"
            "fr" -> "French"
            "es" -> "Spanish"
            else -> "English"
        }

        return "Generate a short, inspiring motivational quote for programmers or developers. " +
            "The quote should be in $languageName. " +
            "Return ONLY the quote text without any additional information, attribution, or explanation."
    }

    private fun buildRequestJson(model: String, prompt: String): JSONObject {
        val requestJson = JSONObject()
        requestJson.put("model", model)
        requestJson.put("max_tokens", 100)
        requestJson.put("temperature", 0.7)

        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", "You are a motivational quote generator for programmers and developers.")
            })
            put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            })
        }

        requestJson.put("messages", messages)
        return requestJson
    }

    private fun parseQuote(responseBody: String, language: String): Result<Quote> {
        return try {
            val jsonResponse = JSONObject(responseBody)
            val choices = jsonResponse.getJSONArray("choices")
            if (choices.length() == 0) {
                return Result.failure(Exception("No quote generated"))
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            val content = message.getString("content").trim()
            val cleanedContent = content.replace("^[\"']|[\"']$".toRegex(), "")
            Result.success(Quote(cleanedContent, language = language))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
