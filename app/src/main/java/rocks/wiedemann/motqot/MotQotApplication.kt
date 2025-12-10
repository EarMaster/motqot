package rocks.wiedemann.motqot

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import androidx.work.WorkManager

class MotQotApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Any initialization code can go here
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "motqot_quotes_channel"
        const val PREFS_NAME = "motqot_prefs"
        const val KEY_API_KEY = "api_key"
        const val KEY_API_BASE_URL = "api_base_url"
        const val KEY_API_MODEL = "api_model"
        const val KEY_PROVIDER_PRESET = "provider_preset"
        const val KEY_LANGUAGE = "language_preference"
        const val KEY_ENABLE_NOTIFICATIONS = "enable_notifications"
        const val KEY_NOTIFICATION_HOUR = "notification_hour"
        const val KEY_NOTIFICATION_MINUTE = "notification_minute"
        const val KEY_NOTIFICATION_TIME = "notification_time"
        const val KEY_LAST_QUOTE = "last_quote"
        const val KEY_LAST_QUOTE_DATE = "last_quote_date"
        
        // Default notification time (8:00 AM)
        const val DEFAULT_NOTIFICATION_HOUR = 8
        const val DEFAULT_NOTIFICATION_MINUTE = 0
        
        // Check if notification permission is granted
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // For Android versions below 13 (TIRAMISU), permission is granted at install time
                true
            }
        }
    }
}
