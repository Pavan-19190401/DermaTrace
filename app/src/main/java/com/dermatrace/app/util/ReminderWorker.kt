package com.dermatrace.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.dermatrace.app.MainActivity
import java.util.concurrent.TimeUnit

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        const val CHANNEL_ID = "dermatrace_monitoring_channel"
        const val KEY_LESION_NAME = "lesion_name"

        fun scheduleReminder(context: Context, lesionName: String, weeksInterval: Long = 4) {
            val data = workDataOf(KEY_LESION_NAME to lesionName)
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(weeksInterval * 7, TimeUnit.DAYS)
                .setInputData(data)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "reminder_$lesionName",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override fun doWork(): Result {
        val lesionName = inputData.getString(KEY_LESION_NAME) ?: "Tracked Lesion"
        showNotification(lesionName)
        return Result.success()
    }

    private fun showNotification(lesionName: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Skin Check Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic reminders to photograph tracked skin lesions for longitudinal analysis"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DermaTrace Follow-up Due")
            .setContentText("Time to re-photograph $lesionName to assess longitudinal change.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Time to re-photograph $lesionName to assess longitudinal change. Consistent monthly imaging improves early detection accuracy.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
