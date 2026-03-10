package rocks.wiedemann.motqot.model

import java.time.LocalDate

/**
 * Data class representing a motivational quote
 */
data class Quote(
    val text: String,
    val date: LocalDate = LocalDate.now(),
    val language: String = "en"
)
