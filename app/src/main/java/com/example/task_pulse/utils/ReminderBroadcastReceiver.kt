package com.example.task_pulse.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.task_pulse.ui.MainActivity

import com.example.task_pulse.database.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("task_id", -1)
        val action = intent.action

        if (action == "MARK_COMPLETED" && taskId != -1) {
            CoroutineScope(Dispatchers.IO).launch {
                val db = TaskDatabase.getInstance(context)
                db.taskDao().markTaskAsCompleted(taskId)
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId)
            }
            return
        }

        val title = intent.getStringExtra("title") ?: "Task Reminder"
        val desc = intent.getStringExtra("desc") ?: "You have a task due!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "task_pulse_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Mark as completed action
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            setAction("MARK_COMPLETED")
            putExtra("task_id", taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context, taskId + 1, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_view, "Mark as Done", completePendingIntent)
            .build()

        notificationManager.notify(taskId, notification)
    }
}
