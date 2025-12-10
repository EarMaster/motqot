package rocks.wiedemann.motqot.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit service aligned with OpenAI-compatible chat completion APIs.
 */
interface OpenAiApiService {

    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body requestBody: RequestBody
    ): Response<ResponseBody>
}
