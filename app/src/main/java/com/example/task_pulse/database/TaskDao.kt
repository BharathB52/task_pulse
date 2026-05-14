package com.example.task_pulse.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.task_pulse.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(task: Task)

    @Update
    fun update(task: Task)

    @Delete
    fun delete(task: Task)

    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE isCompleted = :isCompleted ORDER BY deadline ASC")
    fun getTasksByCompletion(isCompleted: Boolean): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE category = :category ORDER BY deadline ASC")
    fun getTasksByCategory(category: String): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :searchQuery || '%'")
    fun searchTasks(searchQuery: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: Int): Task?

    @Query("UPDATE tasks SET isCompleted = 1 WHERE id = :taskId")
    fun markTaskAsCompleted(taskId: Int)
}
