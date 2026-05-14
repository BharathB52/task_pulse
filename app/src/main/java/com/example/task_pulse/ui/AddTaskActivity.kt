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
import java.text.SimpleDateFormat
import java.util.*

class AddTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTaskBinding
    private val viewModel: TaskViewModel by viewModels()

    private val calendar = Calendar.getInstance()
    private var editingTaskId: Int = -1
    private var currentTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editingTaskId = intent.getIntExtra("task_id", -1)

        setupToolbar()
        setupPickers()
        setupSpinners()

        if (editingTaskId != -1) {
            loadTaskData()
        }

        binding.btnSaveTask.setOnClickListener { saveTask() }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        if (editingTaskId != -1) {
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
            val task = viewModel.getTaskById(editingTaskId)
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
            id = if (editingTaskId != -1) editingTaskId else 0,
            title = title,
            description = desc,
            deadline = calendar.timeInMillis,
            priority = priority,
            category = category,
            repeatInterval = repeat,
            isCompleted = currentTask?.isCompleted ?: false
        )

        if (editingTaskId != -1) {
            viewModel.update(task)
            Toast.makeText(this, "Task updated!", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.insert(task)
            Toast.makeText(this, "Task saved!", Toast.LENGTH_SHORT).show()
        }
        
        scheduleReminder(task)
        finish()
    }

    private fun scheduleReminder(task: Task) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderBroadcastReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("title", task.title)
            putExtra("desc", task.description)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            this, task.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (task.deadline > System.currentTimeMillis()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                task.deadline,
                pendingIntent
            )
        }
    }
}
