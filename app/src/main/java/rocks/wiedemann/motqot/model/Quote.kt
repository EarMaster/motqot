package rocks.wiedemann.motqot.model

import java.util.Date

/**
 * Data class representing a motivational quote
 */
data class Quote(
    val text: String,
    val date: Date = Date(),
    val language: String = "en"
)
