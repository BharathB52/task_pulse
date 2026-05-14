package com.example.task_pulse.ui

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.task_pulse.R
import com.example.task_pulse.model.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(private val listener: OnItemClickListener) :
    ListAdapter<Task, TaskAdapter.TaskHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskHolder(itemView)
    }

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        val currentTask = getItem(position)
        holder.bind(currentTask)
    }

    inner class TaskHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTaskTitle)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvTaskDesc)
        private val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvDeadline: TextView = itemView.findViewById(R.id.tvDeadline)
        private val tvRepeat: TextView = itemView.findViewById(R.id.tvRepeat)
        private val ivDeadline: ImageView = itemView.findViewById(R.id.ivDeadline)
        private val cbComplete: CheckBox = itemView.findViewById(R.id.cbComplete)

        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onItemClick(getItem(pos))
                }
            }
        }

        fun bind(task: Task) {
            tvTitle.text = task.title
            tvTitle.setTextColor(Color.BLACK)
            
            tvDesc.text = task.description
            tvDesc.setTextColor(Color.parseColor("#616161")) // Darker grey for description
            
            tvPriority.text = task.priority.uppercase()
            tvCategory.text = task.category.uppercase()
            tvRepeat.text = if (task.repeatInterval != "None") task.repeatInterval else ""

            val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
            tvDeadline.text = sdf.format(Date(task.deadline))

            // Overdue highlighting
            val isOverdue = task.deadline < System.currentTimeMillis() && !task.isCompleted
            val secondaryColor = Color.parseColor("#757575")
            if (isOverdue) {
                tvDeadline.setTextColor(Color.RED)
                ivDeadline.setColorFilter(Color.RED)
            } else {
                tvDeadline.setTextColor(secondaryColor)
                ivDeadline.setColorFilter(secondaryColor)
            }

            // Update priority background color
            val background = tvPriority.background
            if (background is GradientDrawable) {
                val bgShape = background.mutate() as GradientDrawable
                val color = when (task.priority) {
                    "High" -> "#F44336" // Red
                    "Medium" -> "#FF9800" // Orange
                    "Low" -> "#4CAF50" // Green
                    else -> "#757575" // Grey
                }
                bgShape.setColor(Color.parseColor(color))
            }

            cbComplete.setOnCheckedChangeListener(null)
            cbComplete.isChecked = task.isCompleted
            cbComplete.setOnCheckedChangeListener { _, isChecked ->
                listener.onStatusChanged(task.copy(isCompleted = isChecked))
            }
            
            // Strike through if completed
            if (task.isCompleted) {
                tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tvTitle.alpha = 0.6f
                tvDesc.alpha = 0.6f
            } else {
                tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tvTitle.alpha = 1.0f
                tvDesc.alpha = 1.0f
            }
        }
    }

    interface OnItemClickListener {
        fun onItemClick(task: Task)
        fun onStatusChanged(task: Task)
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }
}
