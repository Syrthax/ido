package com.ido.app.data.model

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Task data model matching the web app schema v2.0
 * 
 * All fields are required and always present to ensure consistency
 * with the web app JSON schema.
 */
@Serializable
data class Task(
    val id: String,
    val text: String,
    val done: Boolean,
    val priority: Boolean,
    val dueDate: String?,  // ISO-8601 or null
    val reminderTime: String?,  // ISO-8601 or null
    val notified: Boolean,
    val createdAt: String,  // ISO-8601
    val updatedAt: String,  // ISO-8601
    val deleted: Boolean
) {
    companion object {
        /**
         * Create a new task with all required fields
         */
        fun create(
            text: String,
            priority: Boolean = false,
            dueDate: String? = null,
            reminderTime: String? = null
        ): Task {
            val now = Instant.now().toString()
            return Task(
                id = UUID.randomUUID().toString(),
                text = text.trim(),
                done = false,
                priority = priority,
                dueDate = dueDate,
                reminderTime = reminderTime,
                notified = false,
                createdAt = now,
                updatedAt = now,
                deleted = false
            )
        }
    }
    
    /**
     * Update task with new values and refresh updatedAt timestamp
     */
    fun update(
        text: String? = null,
        done: Boolean? = null,
        priority: Boolean? = null,
        dueDate: String? = this.dueDate,
        reminderTime: String? = this.reminderTime,
        notified: Boolean? = null
    ): Task {
        return copy(
            text = text ?: this.text,
            done = done ?: this.done,
            priority = priority ?: this.priority,
            dueDate = dueDate,
            reminderTime = reminderTime,
            notified = notified ?: this.notified,
            updatedAt = Instant.now().toString()
        )
    }
    
    /**
     * Mark task as deleted (soft delete)
     */
    fun markDeleted(): Task {
        return copy(
            deleted = true,
            updatedAt = Instant.now().toString()
        )
    }
    
    /**
     * Check if task is overdue
     */
    fun isOverdue(): Boolean {
        if (done || dueDate == null) return false
        return try {
            Instant.parse(dueDate).isBefore(Instant.now())
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get due date as Instant
     */
    fun getDueDateInstant(): Instant? {
        return try {
            dueDate?.let { Instant.parse(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get reminder time as Instant
     */
    fun getReminderTimeInstant(): Instant? {
        return try {
            reminderTime?.let { Instant.parse(it) }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Task collection wrapper matching web app schema
 */
@Serializable
data class TaskCollection(
    val tasks: List<Task>
) {
    companion object {
        fun empty() = TaskCollection(emptyList())
    }
}

/**
 * Extension functions for task lists
 */
fun List<Task>.activeTasks(): List<Task> = filter { !it.deleted }

fun List<Task>.deletedTasks(): List<Task> = filter { it.deleted }

fun List<Task>.sortedByPriority(): List<Task> {
    return sortedWith(
        compareByDescending<Task> { it.priority }
            .thenBy { it.getDueDateInstant() ?: Instant.MAX }
            .thenByDescending { it.createdAt }
    )
}

fun List<Task>.findById(id: String): Task? = find { it.id == id }
