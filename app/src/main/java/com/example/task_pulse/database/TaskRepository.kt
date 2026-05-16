package com.example.task_pulse.database

import com.example.task_pulse.model.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class TaskRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val tasksCollection
        get() = auth.currentUser?.let {
            firestore.collection("users").document(it.uid).collection("tasks")
        }

    fun getAllTasks(): Flow<List<Task>> = callbackFlow {
        val collection = tasksCollection
        if (collection == null) {
            trySend(emptyList())
            // Don't close yet, user might log in later (though we usually recreate VM)
            // But for now, let's just return empty
            return@callbackFlow
        }

        val subscription = collection.orderBy("deadline", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                val tasks = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(tasks)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getTaskById(taskId: String): Task? {
        return try {
            tasksCollection?.document(taskId)?.get()?.await()?.toObject(Task::class.java)?.copy(id = taskId)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insert(task: Task): String {
        val collection = tasksCollection ?: throw Exception("User not logged in")
        val docRef = collection.document()
        val taskWithId = task.copy(id = docRef.id)
        docRef.set(taskWithId).await()
        return docRef.id
    }

    suspend fun update(task: Task) {
        val collection = tasksCollection ?: throw Exception("User not logged in")
        collection.document(task.id).set(task).await()
    }

    suspend fun delete(task: Task) {
        val collection = tasksCollection ?: throw Exception("User not logged in")
        collection.document(task.id).delete().await()
    }
}
