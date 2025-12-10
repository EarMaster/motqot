package rocks.wiedemann.motqot

import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import rocks.wiedemann.motqot.api.ProviderPresets
import rocks.wiedemann.motqot.worker.DailyQuoteWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    
    private val TAG = "SettingsFragment"
    private lateinit var sharedPreferences: SharedPreferences
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Use the standard preferences XML file
        setPreferencesFromResource(R.xml.basic_preferences, rootKey)
        
        // Get shared preferences
        sharedPreferences = requireActivity().getSharedPreferences(
            MotQotApplication.PREFS_NAME, Context.MODE_PRIVATE)

        ensureBaseConfigInitialized()
        
        // Set up time preference
        val timePref = findPreference<Preference>("notification_time")
        timePref?.setOnPreferenceClickListener {
            showTimePickerDialog()
            true
        }
        
        // Update summaries
        updateSummaries()
    }
    
    private fun updateSummaries() {
        // Provider preset
        val presetPref = findPreference<ListPreference>(MotQotApplication.KEY_PROVIDER_PRESET)
        val presetValue = sharedPreferences.getString(
            MotQotApplication.KEY_PROVIDER_PRESET,
            ProviderPresets.PRESET_PERPLEXITY
        ) ?: ProviderPresets.PRESET_PERPLEXITY
        val presetEntry = presetPref?.entries?.getOrNull(
            presetPref.findIndexOfValue(presetValue)
        )
        presetPref?.summary = presetEntry?.toString()

        // API Key
        val apiKeyPref = findPreference<EditTextPreference>(MotQotApplication.KEY_API_KEY)
        val apiKey = sharedPreferences.getString(MotQotApplication.KEY_API_KEY, "")
        if (!apiKey.isNullOrEmpty()) {
            apiKeyPref?.summary = getString(R.string.api_key_set)
        } else {
            apiKeyPref?.summary = getString(R.string.api_key_summary)
        }

        // Base URL
        val baseUrlPref = findPreference<EditTextPreference>(MotQotApplication.KEY_API_BASE_URL)
        val baseUrl = sharedPreferences.getString(MotQotApplication.KEY_API_BASE_URL, null)
        baseUrlPref?.summary = baseUrl ?: getString(R.string.api_base_url_summary)

        // Model
        val modelPref = findPreference<EditTextPreference>(MotQotApplication.KEY_API_MODEL)
        val model = sharedPreferences.getString(MotQotApplication.KEY_API_MODEL, null)
        modelPref?.summary = model ?: getString(R.string.api_model_summary)
        
        // Language
        val languagePref = findPreference<ListPreference>(MotQotApplication.KEY_LANGUAGE)
        val language = sharedPreferences.getString(MotQotApplication.KEY_LANGUAGE, "en") ?: "en"
        val languageName = when(language) {
            "en" -> "English"
            "de" -> "Deutsch"
            "fr" -> "Français"
            "es" -> "Español"
            else -> "English"
        }
        languagePref?.summary = languageName
        
        // Time
        val timePref = findPreference<Preference>(MotQotApplication.KEY_NOTIFICATION_TIME)
        val hour = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_HOUR, 
            MotQotApplication.DEFAULT_NOTIFICATION_HOUR)
        val minute = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_MINUTE, 
            MotQotApplication.DEFAULT_NOTIFICATION_MINUTE)
        val timeString = String.format("%02d:%02d", hour, minute)
        timePref?.summary = timeString
    }
    
    private fun showTimePickerDialog() {
        val hour = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_HOUR, 
            MotQotApplication.DEFAULT_NOTIFICATION_HOUR)
        val minute = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_MINUTE, 
            MotQotApplication.DEFAULT_NOTIFICATION_MINUTE)
        
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minuteOfHour ->
                sharedPreferences.edit()
                    .putInt(MotQotApplication.KEY_NOTIFICATION_HOUR, hourOfDay)
                    .putInt(MotQotApplication.KEY_NOTIFICATION_MINUTE, minuteOfHour)
                    .apply()
                updateSummaries()
                scheduleNotifications()
            },
            hour,
            minute,
            true
        ).show()
    }
    
    private fun scheduleNotifications() {
        val notificationsEnabled = sharedPreferences.getBoolean(
            MotQotApplication.KEY_ENABLE_NOTIFICATIONS, true)
        
        if (notificationsEnabled) {
            val hour = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_HOUR, 
                MotQotApplication.DEFAULT_NOTIFICATION_HOUR)
            val minute = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_MINUTE, 
                MotQotApplication.DEFAULT_NOTIFICATION_MINUTE)
            
            // Create notification data
            val data = Data.Builder()
                .putInt("hour", hour)
                .putInt("minute", minute)
                .build()
            
            // Calculate initial delay
            val currentCalendar = Calendar.getInstance()
            val targetCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                if (before(currentCalendar)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            
            val initialDelay = targetCalendar.timeInMillis - currentCalendar.timeInMillis
            
            // Schedule the worker
            val workRequest = PeriodicWorkRequestBuilder<DailyQuoteWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
            
            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "daily_quote_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                workRequest
            )
            
            Log.d(TAG, "Scheduled notifications for $hour:$minute with initial delay ${initialDelay/1000/60} minutes")
        } else {
            cancelNotifications()
        }
    }
    
    private fun cancelNotifications() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork("daily_quote_work")
        Log.d(TAG, "Cancelled notifications")
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            MotQotApplication.KEY_API_KEY,
            MotQotApplication.KEY_LANGUAGE,
            MotQotApplication.KEY_API_BASE_URL,
            MotQotApplication.KEY_API_MODEL -> updateSummaries()

            MotQotApplication.KEY_PROVIDER_PRESET -> {
                val presetValue = sharedPreferences.getString(key, ProviderPresets.PRESET_PERPLEXITY)
                    ?: ProviderPresets.PRESET_PERPLEXITY
                applyPreset(presetValue)
                updateSummaries()
            }
            
            MotQotApplication.KEY_ENABLE_NOTIFICATIONS -> {
                val enabled = sharedPreferences.getBoolean(key, true)
                if (enabled) {
                    scheduleNotifications()
                } else {
                    cancelNotifications()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }

    private fun ensureBaseConfigInitialized() {
        val baseUrl = sharedPreferences.getString(MotQotApplication.KEY_API_BASE_URL, null)
        val model = sharedPreferences.getString(MotQotApplication.KEY_API_MODEL, null)
        if (!baseUrl.isNullOrBlank() && !model.isNullOrBlank()) {
            return
        }

        val preset = sharedPreferences.getString(
            MotQotApplication.KEY_PROVIDER_PRESET,
            ProviderPresets.PRESET_PERPLEXITY
        ) ?: ProviderPresets.PRESET_PERPLEXITY
        applyPreset(preset)
    }

    private fun applyPreset(presetId: String) {
        if (presetId == ProviderPresets.PRESET_CUSTOM) {
            return
        }

        val preset = ProviderPresets.getPreset(presetId) ?: return
        sharedPreferences.edit()
            .putString(MotQotApplication.KEY_API_BASE_URL, preset.baseUrl)
            .putString(MotQotApplication.KEY_API_MODEL, preset.model)
            .apply()
    }
}
