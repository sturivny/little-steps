package com.example.littlesteps

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Locale
import java.util.concurrent.TimeUnit

object Reminders {
    fun schedule(context: Context, habit: Habit) {
        val manager = WorkManager.getInstance(context)
        val key = "reminder-${habit.id}"
        if (habit.reminder == null || habit.archived != null) { manager.cancelUniqueWork(key); return }
        val now = ZonedDateTime.now()
        var next = now.withHour(habit.reminder / 60).withMinute(habit.reminder % 60).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val work = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("id" to habit.id)).build()
        manager.enqueueUniquePeriodicWork(key, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, work)
    }
}
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val dao = LittleDatabase.get(context).habits()
        val habit = dao.habit(inputData.getLong("id", -1)) ?: return Result.success()
        val today = java.time.LocalDate.now()
        if (habit.reminder == null || !habit.visible(today) || isComplete(habit, dao.entry(habit.id, today.toEpochDay()))) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val language = context.preferences.data.first()[languageKey] ?: "en"
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val localized = context.createConfigurationContext(config)
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "daily-$language"
        manager.createNotificationChannel(NotificationChannel(channelId, localized.getString(R.string.reminders), NotificationManager.IMPORTANCE_DEFAULT))
        val intent = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(habit.id.toInt(), NotificationCompat.Builder(localized, channelId)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(habit.name)
            .setContentText(localized.getString(R.string.reminder_body)).setContentIntent(intent).setAutoCancel(true).build())
        return Result.success()
    }
}
