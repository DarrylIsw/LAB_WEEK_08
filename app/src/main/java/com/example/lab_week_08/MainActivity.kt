package com.example.lab_week_08

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        // WorkManager chain + network constraints
        val workManager = WorkManager.getInstance(this)
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // ✅ pass unique ids to each worker so they can behave independently
        val firstRequest = OneTimeWorkRequest.Builder(FirstWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, "001"))
            .build()

        val secondRequest = OneTimeWorkRequest.Builder(SecondWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, "002"))
            .build()

        val thirdRequest = OneTimeWorkRequest.Builder(ThirdWorker::class.java)
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, "003"))
            .build()

        // Chain the workers: First -> Second -> Third
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .then(thirdRequest)
            .enqueue()

        // Observe and launch services when appropriate
        workManager.getWorkInfoByIdLiveData(firstRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("First process is done")
                    // If you want NotificationService for 001, you can launch here.
                    // launchNotificationService("001")
                }
            }

        workManager.getWorkInfoByIdLiveData(secondRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Second process is done")
                    // Launch NotificationService for 002 — startService (not startForegroundService)
                    launchNotificationService("002")
                }
            }

        workManager.getWorkInfoByIdLiveData(thirdRequest.id)
            .observe(this) { info ->
                if (info.state.isFinished) {
                    showResult("Third process is done")
                    // Launch SecondNotificationService for 003 — this one uses foreground behavior
                    launchSecondNotificationService("003")
                }
            }
    }

    private fun getIdInputData(idKey: String, idValue: String) =
        Data.Builder().putString(idKey, idValue).build()

    private fun showResult(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Launch NotificationService for the given id.
     * Use startForegroundService ONLY for ids that will call startForeground() (like "001").
     * For other ids, use startService so system does not require startForeground().
     */
    private fun launchNotificationService(id: String) {
        val intent = Intent(this, NotificationService::class.java)
        intent.putExtra(NotificationService.EXTRA_ID, id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Only startForegroundService for ids that call startForeground in your service
            if (id == "001") {
                ContextCompat.startForegroundService(this, intent)
            } else {
                // for "002" use regular startService (Activity is foreground here)
                startService(intent)
            }
        } else {
            // pre-O: startService is fine
            startService(intent)
        }
    }

    /**
     * Launch SecondNotificationService for the given id.
     * This service does call startForeground when id == "003", so use startForegroundService.
     */
    private fun launchSecondNotificationService(id: String) {
        val intent = Intent(this, SecondNotificationService::class.java)
        intent.putExtra(SecondNotificationService.EXTRA_ID, id)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }

    companion object {
        const val EXTRA_ID = "Id"
        const val REQUEST_NOTIFICATION_PERMISSION = 1001
    }
}
