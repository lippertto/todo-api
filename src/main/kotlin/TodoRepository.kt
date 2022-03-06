package dev.lippertto.todo

class TodoRepository {

    fun get(id: String): Todo? {
        return Todo(id, "a name", "a description")
    }
}