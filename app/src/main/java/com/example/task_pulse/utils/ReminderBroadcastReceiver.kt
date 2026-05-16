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
import com.example.task_pulse.database.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id")
        val action = intent.action
        
        android.util.Log.d("TaskPulse", "Received broadcast: action=$action, taskId=$taskId")

        if (action == "MARK_COMPLETED" && taskId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = TaskRepository()
                val task = repository.getTaskById(taskId)
                task?.let {
                    repository.update(it.copy(isCompleted = true))
                }
                
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId.hashCode())
                android.util.Log.d("TaskPulse", "Task $taskId marked as completed from notification")
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
        val pendingIntent = PendingIntent.getActivity(context, taskId.hashCode(), mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Mark as completed action
        val completeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            setAction("MARK_COMPLETED")
            putExtra("task_id", taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context, taskId.hashCode() + 1000, completeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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

        android.util.Log.d("TaskPulse", "Showing notification for task $taskId")
        notificationManager.notify(taskId.hashCode(), notification)
    }
}
