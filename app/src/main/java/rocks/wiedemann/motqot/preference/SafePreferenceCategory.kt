package rocks.wiedemann.motqot.preference

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceCategory
import rocks.wiedemann.motqot.R

class SafePreferenceCategory @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : PreferenceCategory(context, attrs) {

    init {
        layoutResource = R.layout.preference_category
    }
}
