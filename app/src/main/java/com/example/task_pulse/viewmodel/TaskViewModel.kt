package com.example.task_pulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.task_pulse.database.TaskDatabase
import com.example.task_pulse.database.TaskRepository
import com.example.task_pulse.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository
    
    private val _currentFilter = MutableStateFlow<FilterType>(FilterType.All)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: Flow<List<Task>>

    init {
        val taskDao = TaskDatabase.getInstance(application).taskDao()
        repository = TaskRepository(taskDao)
        
        tasks = combine(repository.getAllTasks(), _currentFilter, _searchQuery) { allTasks, filter, query ->
            allTasks.filter { task ->
                val matchesFilter = when (filter) {
                    is FilterType.All -> true
                    is FilterType.Completed -> task.isCompleted
                    is FilterType.Pending -> !task.isCompleted
                    is FilterType.Category -> task.category.equals(filter.category, ignoreCase = true)
                }
                val matchesSearch = query.isEmpty() || 
                        task.title.contains(query, ignoreCase = true) || 
                        task.description.contains(query, ignoreCase = true)
                
                matchesFilter && matchesSearch
            }
        }
    }

    fun insert(task: Task) = viewModelScope.launch { repository.insert(task) }
    fun update(task: Task) = viewModelScope.launch { repository.update(task) }
    fun delete(task: Task) = viewModelScope.launch { repository.delete(task) }
    
    suspend fun getTaskById(taskId: Int): Task? = repository.getTaskById(taskId)
    
    fun setFilter(filterType: FilterType) {
        _currentFilter.value = filterType
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    sealed class FilterType {
        object All : FilterType()
        object Completed : FilterType()
        object Pending : FilterType()
        data class Category(val category: String) : FilterType()
    }
}
