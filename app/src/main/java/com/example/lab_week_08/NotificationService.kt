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

class NotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val channelId = "countdown_channel"
    private val completionChannelId = "completion_channel" // for popup notifications

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createCompletionChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID)
        if (id != null) {
            // Launch each countdown independently
            serviceScope.launch { startCountdown(id) }
        }
        return START_NOT_STICKY
    }

    /**
     * Runs a countdown for a specific task ID, showing live updates and a completion popup.
     */
    private suspend fun startCountdown(id: String) {
        val notificationManager = NotificationManagerCompat.from(this)
        val notifId = when (id) {
            "001" -> 0xCA7
            "002" -> 0xCA9
            else -> (System.currentTimeMillis() % 100000).toInt()
        }

        // Create a fresh builder for each task
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Worker Task $id Running")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Start as foreground only for the first one
        if (id == "001") {
            startForeground(notifId, builder.setContentText("Starting countdown...").build())
        }

        // Countdown updates
        for (i in 10 downTo 0) {
            builder.setContentText("Task $id: $i seconds remaining...")
            if (notificationsAllowed()) {
                notificationManager.notify(notifId, builder.build())
            }
            delay(1000)
        }

        // Notify LiveData observers
        withContext(Dispatchers.Main) {
            mutableID.value = id
        }

        // Show popup completion
        showCompletionNotification(id)

        // Stop foreground only for the first task
        if (id == "001") stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /**
     * Checks notification permission safely.
     */
    private fun notificationsAllowed(): Boolean {
        return NotificationManagerCompat.from(this).areNotificationsEnabled() ||
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    this.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                else true
    }

    /**
     * Popup notification shown when countdown finishes.
     */
    private fun showCompletionNotification(id: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val completionNotification = NotificationCompat.Builder(this, completionChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Countdown Complete!")
            .setContentText("Task with ID $id has finished.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = NotificationManagerCompat.from(this)
        if (notificationsAllowed()) {
            notificationManager.notify(id.hashCode(), completionNotification)
        }
    }

    /**
     * Channel for running countdown notifications.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Countdown Notification",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Displays countdown for worker tasks"
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * Channel for popup completion notifications.
     */
    private fun createCompletionChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                completionChannelId,
                "Countdown Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when the countdown is finished"
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
