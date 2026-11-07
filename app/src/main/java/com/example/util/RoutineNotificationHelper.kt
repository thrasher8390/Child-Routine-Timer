package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

data class NotificationSettings(
    val masterEnabled: Boolean = true,
    val startTimeAlerts: Boolean = true,
    val familyStartAlerts: Boolean = true,
    val departureAlerts: Boolean = true
)

class RoutineNotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ROUTINE_REMINDERS = "channel_routine_reminders"
        const val CHANNEL_FAMILY_SYNC = "channel_family_sync"

        const val NOTIFICATION_ID_START_TIME = 1001
        const val NOTIFICATION_ID_FAMILY_START = 1002
        const val NOTIFICATION_ID_DEPARTURE = 1003

        private const val PREFS_NAME = "launchpad_notification_prefs"
        private const val KEY_MASTER = "notif_master_enabled"
        private const val KEY_START_TIME = "notif_start_time_alerts"
        private const val KEY_FAMILY_START = "notif_family_start_alerts"
        private const val KEY_DEPARTURE = "notif_departure_alerts"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannels()
    }

    fun getSettings(): NotificationSettings {
        return NotificationSettings(
            masterEnabled = prefs.getBoolean(KEY_MASTER, true),
            startTimeAlerts = prefs.getBoolean(KEY_START_TIME, true),
            familyStartAlerts = prefs.getBoolean(KEY_FAMILY_START, true),
            departureAlerts = prefs.getBoolean(KEY_DEPARTURE, true)
        )
    }

    fun saveSettings(settings: NotificationSettings) {
        prefs.edit()
            .putBoolean(KEY_MASTER, settings.masterEnabled)
            .putBoolean(KEY_START_TIME, settings.startTimeAlerts)
            .putBoolean(KEY_FAMILY_START, settings.familyStartAlerts)
            .putBoolean(KEY_DEPARTURE, settings.departureAlerts)
            .apply()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // 1. Channel for Routine Start Time and Departure Reminders
            val routineChannel = NotificationChannel(
                CHANNEL_ROUTINE_REMINDERS,
                "Routine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when scheduled routine start time arrives or departure approaches"
                enableVibration(true)
                enableLights(true)
            }

            // 2. Channel for Family Sync Notifications
            val familyChannel = NotificationChannel(
                CHANNEL_FAMILY_SYNC,
                "Family Routine Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when household members start or progress a routine"
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(routineChannel)
            notificationManager.createNotificationChannel(familyChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        val managerCompat = NotificationManagerCompat.from(context)
        if (!managerCompat.areNotificationsEnabled()) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    private fun getPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifyStartTimeArrived(routineTitle: String, startTimeFormatted: String) {
        val settings = getSettings()
        if (!settings.masterEnabled || !settings.startTimeAlerts) return
        if (!hasNotificationPermission()) return

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ROUTINE_REMINDERS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⏰ Time for $routineTitle!")
                .setContentText("It's $startTimeFormatted — target start time has arrived! Tap to start.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(getPendingIntent())
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_START_TIME, builder.build())
        } catch (e: SecurityException) {
            Log.w("RoutineNotification", "Missing notification permission", e)
        } catch (e: Exception) {
            Log.e("RoutineNotification", "Error posting start time notification", e)
        }
    }

    fun notifyFamilyRoutineStarted(routineTitle: String, familyCode: String, totalTasks: Int) {
        val settings = getSettings()
        if (!settings.masterEnabled || !settings.familyStartAlerts) return
        if (!hasNotificationPermission()) return

        try {
            val content = if (familyCode.isNotEmpty()) {
                "Family ($familyCode): $routineTitle is now in progress with $totalTasks tasks. Tap to follow along!"
            } else {
                "$routineTitle is now in progress with $totalTasks tasks. Tap to join!"
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_FAMILY_SYNC)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🟢 Routine Started: $routineTitle")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setContentIntent(getPendingIntent())
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_FAMILY_START, builder.build())
        } catch (e: SecurityException) {
            Log.w("RoutineNotification", "Missing notification permission", e)
        } catch (e: Exception) {
            Log.e("RoutineNotification", "Error posting family routine start notification", e)
        }
    }

    fun notifyDepartureApproaching(routineTitle: String, minutesRemaining: Int) {
        val settings = getSettings()
        if (!settings.masterEnabled || !settings.departureAlerts) return
        if (!hasNotificationPermission()) return

        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ROUTINE_REMINDERS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🎯 Target Departure Approaching")
                .setContentText("$minutesRemaining minutes left before target departure for $routineTitle!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(getPendingIntent())
                .setAutoCancel(true)

            val manager = NotificationManagerCompat.from(context)
            manager.notify(NOTIFICATION_ID_DEPARTURE, builder.build())
        } catch (e: SecurityException) {
            Log.w("RoutineNotification", "Missing notification permission", e)
        } catch (e: Exception) {
            Log.e("RoutineNotification", "Error posting departure warning notification", e)
        }
    }

    fun cancelAll() {
        val manager = NotificationManagerCompat.from(context)
        manager.cancelAll()
    }
}
