package com.example.finance.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.finance.data.AppDatabase
import java.util.concurrent.TimeUnit

class BillReminderWorker(ctx: Context, p: WorkerParameters) : CoroutineWorker(ctx, p) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).dao()
        val now = System.currentTimeMillis()
        val horizon = now + TimeUnit.DAYS.toMillis(30)
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("bills", "Bills", NotificationManager.IMPORTANCE_HIGH))
        dao.dueBefore(horizon).forEach { b ->
            val remindFrom = b.dueDateMillis - TimeUnit.DAYS.toMillis(b.remindDaysBefore.toLong())
            if (now >= remindFrom) {
                val n = NotificationCompat.Builder(applicationContext, "bills")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(b.title).setContentText("${b.amount}")
                    .setAutoCancel(true).build()
                nm.notify(b.id.toInt(), n)
            }
        }
        return Result.success()
    }
    companion object {
        fun schedule(ctx: Context) = WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            "bill_reminders", ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<BillReminderWorker>(1, TimeUnit.DAYS).build())
    }
}
