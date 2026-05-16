package com.example.task_pulse.ui

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.task_pulse.R
import com.example.task_pulse.databinding.ActivityAddTaskBinding
import com.example.task_pulse.model.Task
import com.example.task_pulse.utils.ReminderBroadcastReceiver
import com.example.task_pulse.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    private val viewModel: TaskViewModel by viewModels()

    private val calendar = Calendar.getInstance()
    private var editingTaskId: String? = null
    private var currentTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingTaskId = intent.getStringExtra("task_id")

        setupToolbar()
        setupPickers()
        setupSpinners()

        if (editingTaskId != null) {
            loadTaskData()
        }

        binding.btnSaveTask.setOnClickListener { saveTask() }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        if (editingTaskId != null) {
            binding.toolbar.title = "Edit Task"
            binding.btnSaveTask.text = "Update Task"
        }
    }

    private fun setupPickers() {
        binding.btnPickDate.setOnClickListener {
            DatePickerDialog(this, { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateLabel()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.btnPickTime.setOnClickListener {
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                updateTimeLabel()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show()
        }
        
        updateDateLabel()
        updateTimeLabel()
    }

    private fun updateDateLabel() {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        binding.tvSelectedDate.text = sdf.format(calendar.time)
    }

    private fun updateTimeLabel() {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        binding.tvSelectedTime.text = sdf.format(calendar.time)
    }

    private fun setupSpinners() {
        val categories = arrayOf("Personal", "Work", "Study")
        val catAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = catAdapter

        val repeatIntervals = arrayOf("None", "Daily", "Weekly", "Custom")
        val repAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, repeatIntervals)
        repAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerRepeat.adapter = repAdapter
    }

    private fun loadTaskData() {
        lifecycleScope.launch {
            val task = editingTaskId?.let { viewModel.getTaskById(it) }
            task?.let {
                if (currentTask == null) { // Only load once to avoid overwriting user edits
                    currentTask = it
                    binding.etTaskTitle.setText(it.title)
                    binding.etTaskDesc.setText(it.description)
                    calendar.timeInMillis = it.deadline
                    updateDateLabel()
                    updateTimeLabel()
                    
                    when (it.priority) {
                        "High" -> binding.rbHigh.isChecked = true
                        "Medium" -> binding.rbMedium.isChecked = true
                        else -> binding.rbLow.isChecked = true
                    }
                    
                    val categories = arrayOf("Personal", "Work", "Study")
                    val catIndex = categories.indexOf(it.category)
                    if (catIndex >= 0) binding.spinnerCategory.setSelection(catIndex)
                    
                    val repeatIntervals = arrayOf("None", "Daily", "Weekly", "Custom")
                    val repIndex = repeatIntervals.indexOf(it.repeatInterval)
                    if (repIndex >= 0) binding.spinnerRepeat.setSelection(repIndex)
                }
            }
        }
    }

    private fun saveTask() {
        val title = binding.etTaskTitle.text.toString().trim()
        val desc = binding.etTaskDesc.text.toString().trim()
        val priority = when (binding.rgPriority.checkedRadioButtonId) {
            R.id.rbHigh -> "High"
            R.id.rbMedium -> "Medium"
            else -> "Low"
        }
        val category = binding.spinnerCategory.selectedItem.toString()
        val repeat = binding.spinnerRepeat.selectedItem.toString()

        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show()
            return
        }

        val task = Task(
            id = editingTaskId ?: "",
            title = title,
            description = desc,
            deadline = calendar.timeInMillis,
            priority = priority,
            category = category,
            repeatInterval = repeat,
            isCompleted = currentTask?.isCompleted ?: false
        )

        if (editingTaskId != null) {
            viewModel.update(task)
            Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show()
            scheduleReminder(task)
            finish()
        } else {
            viewModel.insert(task) { newId ->
                val newTask = task.copy(id = newId)
                scheduleReminder(newTask)
                runOnUiThread {
                    Toast.makeText(this@AddTaskActivity, "Task saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun scheduleReminder(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        // Check exact alarm permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.w("TaskPulse", "Exact alarm permission missing. Fallback to inexact alarm.")
                // Optionally request permission or just use inexact
            }
        }

        val intent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("title", task.title)
            putExtra("desc", task.description)
        }
        
        // Request code must be Int, so we use hashCode
        val requestCode = task.id.hashCode()

        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (task.deadline > System.currentTimeMillis()) {
            android.util.Log.d("TaskPulse", "Scheduling alarm for task ID: ${task.id} at ${task.deadline} (Now: ${System.currentTimeMillis()})")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    task.deadline,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    task.deadline,
                    pendingIntent
                )
            }
        } else {
            android.util.Log.w("TaskPulse", "Cannot schedule alarm: deadline is in the past for task ID: ${task.id}")
        }
    }
}
