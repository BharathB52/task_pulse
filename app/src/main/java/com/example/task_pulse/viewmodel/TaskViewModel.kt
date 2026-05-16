package com.example.task_pulse.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.task_pulse.database.TaskRepository
import com.example.task_pulse.model.Task
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository()
    
    private val _currentFilter = MutableStateFlow<FilterType>(FilterType.All)
    private val _searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: Flow<List<Task>>

    val allTasks: StateFlow<List<Task>> = repository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        tasks = combine(allTasks, _currentFilter, _searchQuery) { allTasksList, filter, query ->
            allTasksList.filter { task ->
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

    fun insert(task: Task, onInserted: (String) -> Unit = {}) = viewModelScope.launch { 
        val id = repository.insert(task)
        onInserted(id)
    }
    fun update(task: Task) = viewModelScope.launch { repository.update(task) }
    fun delete(task: Task) = viewModelScope.launch { repository.delete(task) }
    
    suspend fun getTaskById(taskId: String): Task? = repository.getTaskById(taskId)
    
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
