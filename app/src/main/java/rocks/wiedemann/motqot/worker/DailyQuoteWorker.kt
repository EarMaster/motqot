package rocks.wiedemann.motqot.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rocks.wiedemann.motqot.MainActivity
import rocks.wiedemann.motqot.MotQotApplication
import rocks.wiedemann.motqot.R
import rocks.wiedemann.motqot.repository.QuoteRepository
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Worker class for generating and displaying daily motivational quotes
 */
class DailyQuoteWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    private val TAG = "DailyQuoteWorker"
    private val repository = QuoteRepository(context)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting daily quote worker")
            
            // Check if notifications are enabled
            if (!repository.areNotificationsEnabled()) {
                Log.d(TAG, "Notifications are disabled")
                return@withContext Result.success()
            }
            
            // Check if the provider configuration is complete
            if (!repository.hasCompleteApiConfig()) {
                Log.e(TAG, "API provider configuration incomplete")
                return@withContext Result.failure()
            }
            
            // Get the preferred language
            val language = repository.getLanguagePreference()
            
            // Generate a new quote with the preferred language
            val quoteResult = repository.generateQuote(language)
            
            if (quoteResult.isSuccess) {
                val quote = quoteResult.getOrNull()
                if (quote != null) {
                    // Save the quote
                    repository.saveQuote(quote)
                    
                    // Show notification
                    showNotification(quote.text)
                    
                    Log.d(TAG, "Quote generated successfully: ${quote.text}")
                    return@withContext Result.success()
                }
            }
            
            Log.e(TAG, "Failed to generate quote: ${quoteResult.exceptionOrNull()?.message}")
            return@withContext Result.retry()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in DailyQuoteWorker", e)
            return@withContext Result.failure()
        }
    }
    
    private fun showNotification(quoteText: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Check if we have notification permission on Android 13+
        if (!MotQotApplication.hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission not granted, skipping notification")
            return
        }
        
        // Create intent for when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, MotQotApplication.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.notification_title_text))
            .setContentText(quoteText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(quoteText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
