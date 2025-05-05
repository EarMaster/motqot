package rocks.wiedemann.motqot

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import rocks.wiedemann.motqot.databinding.ActivitySettingsBinding
import rocks.wiedemann.motqot.worker.DailyQuoteWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.os.Build

class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
            
            // Set up notification toggle preference
            val notificationPref = findPreference<androidx.preference.SwitchPreferenceCompat>(MotQotApplication.KEY_ENABLE_NOTIFICATIONS)
            notificationPref?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (enabled) {
                    // If enabling notifications, check for permission on Android 13+
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                        !MotQotApplication.hasNotificationPermission(requireContext())) {
                        // Show dialog explaining notification permission
                        android.app.AlertDialog.Builder(requireContext())
                            .setTitle(R.string.notification_permission_required)
                            .setMessage(R.string.notification_permission_settings_message)
                            .setPositiveButton(R.string.open_settings) { _, _ ->
                                // Open app settings so user can enable notifications
                                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                                startActivity(intent)
                            }
                            .setNegativeButton(R.string.cancel, null)
                            .show()
                    }
                }
                true // Return true to update the state of the Preference
            }
            
            // Set up time preference
            val timePref = findPreference<Preference>(MotQotApplication.KEY_NOTIFICATION_TIME)
            timePref?.setOnPreferenceClickListener {
                showTimePickerDialog()
                true
            }
            
            // Update time preference summary
            val prefs = requireContext().getSharedPreferences(
                MotQotApplication.PREFS_NAME, 
                MODE_PRIVATE
            )
            val hour = prefs.getInt(
                "notification_hour", 
                MotQotApplication.DEFAULT_NOTIFICATION_HOUR
            )
            val minute = prefs.getInt(
                "notification_minute", 
                MotQotApplication.DEFAULT_NOTIFICATION_MINUTE
            )
            
            timePref?.summary = String.format("%02d:%02d", hour, minute)
        }
        
        private fun showTimePickerDialog() {
            val prefs = requireContext().getSharedPreferences(
                MotQotApplication.PREFS_NAME, 
                MODE_PRIVATE
            )
            
            val hour = prefs.getInt(
                "notification_hour", 
                MotQotApplication.DEFAULT_NOTIFICATION_HOUR
            )
            val minute = prefs.getInt(
                "notification_minute", 
                MotQotApplication.DEFAULT_NOTIFICATION_MINUTE
            )
            
            TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    // Save selected time
                    prefs.edit()
                        .putInt("notification_hour", selectedHour)
                        .putInt("notification_minute", selectedMinute)
                        .apply()
                    
                    // Update preference summary
                    findPreference<Preference>(MotQotApplication.KEY_NOTIFICATION_TIME)?.summary =
                        String.format("%02d:%02d", selectedHour, selectedMinute)
                    
                    // Schedule daily quote worker
                    scheduleDailyQuoteWorker(selectedHour, selectedMinute)
                },
                hour,
                minute,
                true
            ).show()
        }
        
        private fun scheduleDailyQuoteWorker(hour: Int, minute: Int) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            
            // If the time has already passed today, schedule for tomorrow
            val now = Calendar.getInstance()
            if (calendar.before(now)) {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            // Check notification permission if on Android 13+ 
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
                !MotQotApplication.hasNotificationPermission(requireContext())) {
                // Inform user that notifications require permission
                android.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.notification_permission_required)
                    .setMessage(R.string.notification_permission_settings_message)
                    .setPositiveButton(R.string.open_settings) { _, _ ->
                        // Open app settings so user can enable notifications
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                        startActivity(intent)
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            
            val initialDelay = calendar.timeInMillis - now.timeInMillis
            
            val inputData = Data.Builder()
                .putInt("hour", hour)
                .putInt("minute", minute)
                .build()
            
            val dailyQuoteRequest = PeriodicWorkRequestBuilder<DailyQuoteWorker>(
                24, TimeUnit.HOURS
            )
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build()
            
            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "daily_quote_work",
                ExistingPeriodicWorkPolicy.REPLACE,
                dailyQuoteRequest
            )
        }
    }
}
