package rocks.wiedemann.motqot.api

/**
 * Simple holder for the configurable OpenAI-compatible provider settings.
 */
data class ApiProviderConfig(
    val baseUrl: String,
    val apiKey: String,
    val model: String
)
