package rocks.wiedemann.motqot.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit interface for the Perplexity API
 */
interface PerplexityApiService {
    
    @POST("chat/completions")
    suspend fun generateQuote(
        @Header("Authorization") apiKey: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
    
    companion object {
        const val BASE_URL = "https://api.perplexity.ai/"
    }
}
