package rocks.wiedemann.motqot

import android.app.TimePickerDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import rocks.wiedemann.motqot.worker.DailyQuoteWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "SettingsActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = getString(R.string.settings)
        
        // Add the settings fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun updateSummaries() {
        // Update API key summary
        val apiKey = sharedPreferences.getString(MotQotApplication.KEY_API_KEY, null)
        val apiKeySummary = findViewById<TextView>(R.id.api_key_summary)
        if (!apiKey.isNullOrEmpty()) {
            apiKeySummary.text = getString(R.string.api_key_set)
        } else {
            apiKeySummary.text = getString(R.string.api_key_summary)
        }
        
        // Update language summary
        val language = sharedPreferences.getString(MotQotApplication.KEY_LANGUAGE, "en") ?: "en"
        val languageSummary = findViewById<TextView>(R.id.language_summary)
        val languageName = when(language) {
            "en" -> "English"
            "de" -> "Deutsch"
            "fr" -> "Français"
            "es" -> "Español"
            else -> "English"
        }
        languageSummary.text = languageName
        
        // Update time summary
        val hour = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_HOUR, 8)
        val minute = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_MINUTE, 0)
        val timeSummary = findViewById<TextView>(R.id.notification_time_summary)
        val timeString = String.format("%02d:%02d", hour, minute)
        timeSummary.text = timeString
    }
    
    private fun showApiKeyDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val textInputLayout = view.findViewById<TextInputLayout>(R.id.text_input_layout)
        val editText = view.findViewById<TextInputEditText>(R.id.edit_text)
        
        textInputLayout.hint = getString(R.string.api_key_title)
        editText.setText(sharedPreferences.getString(MotQotApplication.KEY_API_KEY, ""))
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.api_key_dialog_title)
            .setView(view)
            .setPositiveButton(R.string.save) { _, _ ->
                val apiKey = editText.text.toString().trim()
                sharedPreferences.edit().putString(MotQotApplication.KEY_API_KEY, apiKey).apply()
                updateSummaries()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Deutsch", "Français", "Español")
        val values = arrayOf("en", "de", "fr", "es")
        val currentLanguage = sharedPreferences.getString(MotQotApplication.KEY_LANGUAGE, "en") ?: "en"
        val currentIndex = values.indexOf(currentLanguage)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                sharedPreferences.edit().putString(MotQotApplication.KEY_LANGUAGE, values[which]).apply()
                updateSummaries()
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showTimePickerDialog() {
        val hour = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_HOUR, 8)
        val minute = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_MINUTE, 0)
        
        TimePickerDialog(
            this,
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
        val hour = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_HOUR, 8)
        val minute = sharedPreferences.getInt(MotQotApplication.KEY_NOTIFICATION_MINUTE, 0)
        
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
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_quote_work",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
        
        Log.d(TAG, "Scheduled notifications for $hour:$minute with initial delay ${initialDelay/1000/60} minutes")
    }
    
    private fun cancelNotifications() {
        WorkManager.getInstance(this).cancelUniqueWork("daily_quote_work")
        Log.d(TAG, "Cancelled notifications")
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
