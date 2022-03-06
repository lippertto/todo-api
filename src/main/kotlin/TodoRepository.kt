package dev.lippertto.todo

import java.util.UUID

class TodoRepository(private val data: MutableMap<String, Todo> = mutableMapOf()) {

    fun find(id: String): Todo? {
        return data[id]
    }

    fun put(todo: Todo): Todo {
        val newId = UUID.randomUUID()
        val newTodo = todo.copy(id = newId.toString())
        data[newId.toString()] = newTodo
        return newTodo
    }

    fun update(id: String, todo: Todo): Todo? {
        if (find(id) == null) {
            return null;
        }
        val newTodo = todo.copy(id = id)
        data[id] = newTodo
        return newTodo
    }
}