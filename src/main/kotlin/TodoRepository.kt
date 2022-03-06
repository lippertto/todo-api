package dev.lippertto.todo

import java.util.UUID

class TodoRepository(private val data: MutableMap<String, Todo> = mutableMapOf()) {

    fun find(id: String): Todo? {
        return data[id]
    }

    fun save(todo: Todo): Todo {
        val newId = UUID.randomUUID()
        val newTodo = todo.copy(id = newId.toString())
        data[newId.toString()] = newTodo
        return newTodo
    }

    fun updateTodo(id: String, todo: Todo): Todo? {
        if (find(id) == null) {
            return null;
        }
        val newTodo = todo.copy(id = id)
        data[id] = newTodo
        return newTodo
    }

    fun updateTask(todoId: String, taskId: String, task: Task): Task? {
        val todo = find(todoId) ?: return null

        val index = todo.tasks.indexOfFirst { it.id == taskId }
        if (index == -1) {
            return null
        }
        val newTask = task.copy(id = taskId)

        data[todoId] = todo.copy(
            tasks = todo.tasks.toMutableList().also {
                it[index] = newTask
            })
        return newTask
    }

    fun addTask(id: String, task: Task): Task? {
        val todo = data[id] ?: return null

        val newId = UUID.randomUUID()
        val newTask = task.copy(id = newId.toString())
        data[id] = todo.copy(tasks = todo.tasks + newTask)
        return newTask
    }

    fun delete(id: String): Boolean {
        if (find(id) == null) {
            return false
        }
        data.remove(id)
        return true
    }
}