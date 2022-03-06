package dev.lippertto.todo
import kotlinx.serialization.Serializable

@Serializable
data class Task(val id: String, val name: String)

@Serializable
data class Todo(val id: String, val name: String, val description: String, val tasks: List<Task> = listOf())

@Serializable
data class ErrorResponse(val error: ErrorObject)

@Serializable
data class ErrorObject(val code: String, val message: String, val details: String? = null)