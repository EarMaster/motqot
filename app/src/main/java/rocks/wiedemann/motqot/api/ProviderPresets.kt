package rocks.wiedemann.motqot.api

/**
 * Supported provider presets for quick configuration.
 */
object ProviderPresets {

    data class Preset(
        val id: String,
        val baseUrl: String,
        val model: String
    )

    const val PRESET_OPENAI = "openai"
    const val PRESET_OPENROUTER = "openrouter"
    const val PRESET_ANTHROPIC = "anthropic"
    const val PRESET_MISTRAL = "mistral"
    const val PRESET_PERPLEXITY = "perplexity"
    const val PRESET_CUSTOM = "custom"

    private val presets = mapOf(
        PRESET_OPENAI to Preset(
            PRESET_OPENAI,
            baseUrl = "https://api.openai.com/v1/",
            model = "gpt-4o-mini"
        ),
        PRESET_OPENROUTER to Preset(
            PRESET_OPENROUTER,
            baseUrl = "https://openrouter.ai/api/v1/",
            model = "openrouter/auto"
        ),
        PRESET_ANTHROPIC to Preset(
            PRESET_ANTHROPIC,
            baseUrl = "https://api.anthropic.com/v1/",
            model = "claude-3-5-sonnet-20241022"
        ),
        PRESET_MISTRAL to Preset(
            PRESET_MISTRAL,
            baseUrl = "https://api.mistral.ai/v1/",
            model = "mistral-large-latest"
        ),
        PRESET_PERPLEXITY to Preset(
            PRESET_PERPLEXITY,
            baseUrl = "https://api.perplexity.ai/",
            model = "sonar"
        )
    )

    fun getPreset(id: String?): Preset? = presets[id]
}
