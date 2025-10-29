package com.example.lab_week_08

import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*

class SecondNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channelId = "second_countdown_channel"
    private val completionChannelId = "second_completion_channel"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createCompletionChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID)
        if (id != null) serviceScope.launch { startCountdown(id) }
        return START_NOT_STICKY
    }

    private suspend fun startCountdown(id: String) {
        val manager = NotificationManagerCompat.from(this)
        val notifId = getNotifId(id)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Worker Task $id Running")
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)

        if (id == "003") {
            startForeground(notifId, builder.setContentText("Starting final countdown...").build())
        } else {
            manager.notify(notifId, builder.setContentText("Starting final countdown...").build())
        }

        for (i in 5 downTo 0) {
            builder.setContentText("Task $id: $i seconds remaining...")
            if (notificationsAllowed()) manager.notify(notifId, builder.build())
            delay(1000)
        }

        builder.setContentTitle("Worker Task $id Completed")
            .setContentText("Task $id finished successfully ✅")
            .setOngoing(false)

        if (notificationsAllowed()) manager.notify(notifId, builder.build())

        // 🎉 Show the "All Processes Complete" notification
        showFinalAllCompleteNotification(id)

        withContext(Dispatchers.Main) { mutableID.value = id }

        if (id == "003") stopForeground(STOP_FOREGROUND_REMOVE)
        delay(500)
        stopSelf()
    }

    private fun getNotifId(id: String): Int = when (id) {
        "003" -> 1003
        else -> (System.currentTimeMillis() % 100000).toInt()
    }

    private fun notificationsAllowed(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled() ||
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    this.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                else true
    }

    private fun showFinalAllCompleteNotification(id: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 9999, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, completionChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 All Processes Complete!")
            .setContentText("All worker tasks (001–003) have completed successfully.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        val manager = NotificationManagerCompat.from(this)
        if (notificationsAllowed()) manager.notify(9999, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Final Countdown Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Displays countdown for the final worker task"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun createCompletionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                completionChannelId,
                "All Tasks Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when all processes are finished"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val EXTRA_ID = "Id"
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
