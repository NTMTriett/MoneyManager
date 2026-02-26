package com.example.moneymanager.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.example.moneymanager.R // Ensure this matches your package

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val CHANNEL_NAME = "Budget Alerts"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for budget limits"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showBudgetAlert(budgetId: String, budgetName: String, percent: Int) {
        try {
            // Use a system icon if app icon isn't available, or R.drawable.ic_launcher_foreground
            val icon = android.R.drawable.stat_sys_warning

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle("Budget Alert: $budgetName")
                .setContentText("You have reached $percent% of your $budgetName budget.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            with(NotificationManagerCompat.from(context)) {
                // Use budgetId hashcode as notification ID to update existing ones instead of stacking
                notify(budgetId.hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            // Permission not granted
            e.printStackTrace()
        }
    }
}