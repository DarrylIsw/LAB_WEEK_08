package com.example.lab_week_08.worker

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

class SecondWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        // Get the input parameter
        val id = inputData.getString(INPUT_DATA_ID)

        // Simulate a task (e.g., network upload, file write, etc.)
        Thread.sleep(3000L)

        // Build the output data
        val outputData = Data.Builder()
            .putString(OUTPUT_DATA_ID, "Processed second with ID: $id")
            .build()

        // Return success with output data
        return Result.success(outputData)
    }

    companion object {
        const val INPUT_DATA_ID = "inId"
        const val OUTPUT_DATA_ID = "outId"
    }
}
