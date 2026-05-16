package com.example.task_pulse.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.task_pulse.databinding.ActivityAnalyticsBinding
import com.example.task_pulse.viewmodel.TaskViewModel
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnalyticsBinding
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        setupCharts()
        observeData()
    }

    private fun setupCharts() {
        binding.pieChartStatus.description.isEnabled = false
        binding.pieChartStatus.isDrawHoleEnabled = true
        binding.pieChartStatus.setEntryLabelColor(android.graphics.Color.BLACK)
        
        binding.barChartCategory.description.isEnabled = false
        binding.barChartCategory.setDrawGridBackground(false)
        binding.barChartCategory.axisRight.isEnabled = false
        val xAxis = binding.barChartCategory.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allTasks.collectLatest { tasks ->
                    val completed = tasks.count { it.isCompleted }
                    val pending = tasks.size - completed
                    updatePieChart(completed, pending)
                    
                    val workCount = tasks.count { it.category.equals("Work", ignoreCase = true) }
                    val studyCount = tasks.count { it.category.equals("Study", ignoreCase = true) }
                    val personalCount = tasks.count { it.category.equals("Personal", ignoreCase = true) }
                    updateBarChart(workCount, studyCount, personalCount)
                }
            }
        }
    }

    private fun updatePieChart(completed: Int, pending: Int) {
        val entries = ArrayList<PieEntry>()
        if (completed > 0) entries.add(PieEntry(completed.toFloat(), "Completed"))
        if (pending > 0) entries.add(PieEntry(pending.toFloat(), "Pending"))

        // If no data, show empty chart correctly
        if (entries.isEmpty()) {
            binding.pieChartStatus.clear()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            ColorTemplate.rgb("#4CAF50"), // Green for completed
            ColorTemplate.rgb("#FFC107")  // Amber for pending
        )
        dataSet.valueTextSize = 14f
        
        val data = PieData(dataSet)
        binding.pieChartStatus.data = data
        binding.pieChartStatus.invalidate()
    }

    private fun updateBarChart(work: Int, study: Int, personal: Int) {
        val entries = ArrayList<BarEntry>()
        entries.add(BarEntry(0f, work.toFloat()))
        entries.add(BarEntry(1f, study.toFloat()))
        entries.add(BarEntry(2f, personal.toFloat()))

        val dataSet = BarDataSet(entries, "Categories")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 14f

        val data = BarData(dataSet)
        data.barWidth = 0.5f

        binding.barChartCategory.data = data
        binding.barChartCategory.xAxis.valueFormatter = IndexAxisValueFormatter(listOf("Work", "Study", "Personal"))
        binding.barChartCategory.invalidate()
    }
}
