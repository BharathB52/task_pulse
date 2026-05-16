package com.example.task_pulse.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.task_pulse.R
import com.example.task_pulse.databinding.ActivityMainBinding
import com.example.task_pulse.model.Task
import com.example.task_pulse.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), TaskAdapter.OnItemClickListener {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: TaskViewModel by viewModels()
    private lateinit var adapter: TaskAdapter
    private var selectedCalendarDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check Auth
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupProgressAndFilters()
        setupFAB()
        setupSwipe()
        setupCalendar()
        updateGreeting()
        checkNotificationPermission()
        observeTasks()
        
        binding.btnAnalytics?.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }

        // Add Logout listener (using the user icon if we had one, or long click on greeting)
        binding.tvGreeting.setOnLongClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            true
        }
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(this)
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTasks.adapter = adapter
    }

    private fun setupProgressAndFilters() {
        val filters = arrayOf("All Tasks", "Pending", "Completed", "Work", "Study", "Personal")
        val filterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filters)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = filterAdapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, pos: Int, p3: Long) {
                val filter = when (filters[pos]) {
                    "Pending" -> TaskViewModel.FilterType.Pending
                    "Completed" -> TaskViewModel.FilterType.Completed
                    "Work" -> TaskViewModel.FilterType.Category("Work")
                    "Study" -> TaskViewModel.FilterType.Category("Study")
                    "Personal" -> TaskViewModel.FilterType.Category("Personal")
                    else -> TaskViewModel.FilterType.All
                }
                viewModel.setFilter(filter)
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.setSearchQuery(it) }
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { viewModel.setSearchQuery(it) }
                return true
            }
        })
    }

    private fun setupCalendar() {
        binding.layoutCalendar.removeAllViews()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -3) // Show from 3 days ago

        val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
        val sdfDate = SimpleDateFormat("dd", Locale.getDefault())

        for (i in 0..14) { // Show 15 days
            val dateView = LayoutInflater.from(this).inflate(R.layout.calendar_day_item, binding.layoutCalendar, false)
            val tvDay = dateView.findViewById<TextView>(R.id.tvDay)
            val tvDate = dateView.findViewById<TextView>(R.id.tvDate)
            
            val currentDate = cal.timeInMillis
            tvDay.text = sdfDay.format(cal.time)
            tvDate.text = sdfDate.format(cal.time)

            val isSelected = isSameDay(cal, selectedCalendarDate)
            updateDateViewStyle(dateView, isSelected)

            dateView.setOnClickListener {
                selectedCalendarDate.timeInMillis = currentDate
                setupCalendar() // Refresh UI
                // In a real app, we'd filter tasks by this date
                // For now, let's just show a toast
                Toast.makeText(this, "Selected: ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(cal.time)}", Toast.LENGTH_SHORT).show()
            }

            binding.layoutCalendar.addView(dateView)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun updateDateViewStyle(view: View, isSelected: Boolean) {
        val card = view.findViewById<androidx.cardview.widget.CardView>(R.id.cardDate)
        val tvDay = view.findViewById<TextView>(R.id.tvDay)
        val tvDate = view.findViewById<TextView>(R.id.tvDate)

        if (isSelected) {
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.primary_blue))
            tvDay.setTextColor(ContextCompat.getColor(this, R.color.white))
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
            tvDay.setTextColor(ContextCompat.getColor(this, R.color.text_sub))
            tvDate.setTextColor(ContextCompat.getColor(this, R.color.text_main))
        }
    }

    private fun updateGreeting() {
        binding.tvGreeting.text = "Hello"
        binding.tvUserName.text = "user"
        
        val sdf = SimpleDateFormat("EEEE, dd MMMM", Locale.getDefault())
        binding.tvCurrentDate.text = sdf.format(Date())
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Handle Exact Alarm permission for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                android.widget.Toast.makeText(this, "Please enable Alarms & Reminders for TaskPulse in settings", android.widget.Toast.LENGTH_LONG).show()
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                startActivity(intent)
            }
        }
    }

    private fun setupFAB() {
        binding.fabAddTask.setOnClickListener {
            startActivity(Intent(this, AddTaskActivity::class.java))
        }
    }

    private fun observeTasks() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tasks.collect { tasks ->
                        adapter.submitList(tasks)
                    }
                }
                launch {
                    viewModel.allTasks.collect { allTasks ->
                        updateProgress(allTasks)
                    }
                }
            }
        }
    }

    private fun updateProgress(tasks: List<Task>) {
        if (tasks.isEmpty()) {
            binding.progressBar.progress = 0
            binding.tvProgress.text = "0%"
            return
        }
        val completed = tasks.count { it.isCompleted }
        val percent = (completed * 100) / tasks.size
        
        binding.progressBar.progress = percent
        binding.tvProgress.text = "$percent%"
    }

    private fun setupSwipe() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val task = adapter.currentList[viewHolder.adapterPosition]
                if (direction == ItemTouchHelper.LEFT) {
                    viewModel.delete(task)
                    Toast.makeText(this@MainActivity, "Task deleted", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.update(task.copy(isCompleted = !task.isCompleted))
                    val status = if (!task.isCompleted) "completed" else "pending"
                    Toast.makeText(this@MainActivity, "Task marked as $status", Toast.LENGTH_SHORT).show()
                }
            }
        })
        helper.attachToRecyclerView(binding.recyclerViewTasks)
    }

    override fun onItemClick(task: Task) {
        val intent = Intent(this, AddTaskActivity::class.java)
        intent.putExtra("task_id", task.id)
        startActivity(intent)
    }

    override fun onStatusChanged(task: Task) {
        viewModel.update(task)
    }
}
