package dev.lippertto.todo

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.request.ContentTransformationException
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import io.ktor.serialization.json
import kotlinx.serialization.SerializationException

val todoRepo = TodoRepository()

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

private suspend fun returnNotFound (call: ApplicationCall, entity: String) {
    call.respondText(
        Json.encodeToString(
            ErrorResponse(ErrorObject("NotFound", "Could not find entity $entity with given id"))
        ), ContentType.Application.Json, HttpStatusCode.NotFound
    )
}

private suspend fun returnBadRequest(call: ApplicationCall) {
    call.respondText(
        Json.encodeToString(
            ErrorResponse(ErrorObject("BadRequest", "Bad payload provided"))
        ), ContentType.Application.Json, HttpStatusCode.BadRequest
    )
}


fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        json()
    }
    install(StatusPages) {
        exception<SerializationException> {returnBadRequest(call) }
        exception<ContentTransformationException> {returnBadRequest(call) }
    }

    routing {
        createTodoRoute()
        todoByIdRoute()
        updateTodoRoute()
        deleteTodoRoute()

        createTaskRoute()
        taskByIdRoute()
        updateTaskRoute()
        deleteTaskRoute()
    }
}

private fun Route.todoByIdRoute() {
    get("/todos/{todoId}") {
        val todoId = call.parameters["todoId"] as String
        val todo = todoRepo.find(todoId) ?: return@get returnNotFound(call, "Todo")

        call.respondText(Json.encodeToString(todo), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

private fun Route.updateTodoRoute() {
    put("/todos/{todoId}") {
        val todoId = call.parameters["todoId"] as String
        todoRepo.find(todoId) ?: return@put returnNotFound(call, "Todo")

        val todo = call.receive<Todo>()

        val updatedTodo = todoRepo.updateTodo(todoId, todo)
        call.respondText(Json.encodeToString(updatedTodo), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

private fun Route.createTodoRoute() {
    post("/todos") {
        val todo = call.receive<Todo>()

        val newTodo = todoRepo.save(todo)

        call.respondText(Json.encodeToString(newTodo), ContentType.Application.Json, HttpStatusCode.Created)
    }
}

private fun Route.deleteTodoRoute() {
    delete("/todos/{todoId}") {
        val todoId = call.parameters["todoId"] as String
        if (!todoRepo.deleteTodo(todoId)) {
            returnNotFound(call, "Todo")
        } else {
            call.respondText("", ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}

private fun Route.createTaskRoute() {
    post("/todos/{todoId}/tasks") {
        val task = call.receive<Task>()
        val todoId = call.parameters["todoId"] as String
        todoRepo.find(todoId) ?: return@post returnNotFound(call, "Todo")

        val newTask = todoRepo.addTask(todoId, task) ?: return@post returnNotFound(call, "Task")

        call.respondText(Json.encodeToString(newTask), ContentType.Application.Json, HttpStatusCode.Created)
    }
}

private fun Route.updateTaskRoute() {
    put("/todos/{todoId}/tasks/{taskId}") {
        val todoId = call.parameters["todoId"] as String
        todoRepo.find(todoId) ?: return@put returnNotFound(call, "Todo")

        val taskId = call.parameters["taskId"] as String
        val task = call.receive<Task>()

        val newTask = todoRepo.updateTask(todoId, taskId, task) ?: return@put returnNotFound(call, "Task")
        call.respondText(Json.encodeToString(newTask), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

private fun Route.taskByIdRoute() {
    get("/todos/{todoId}/tasks/{taskId}") {
        val todoId = call.parameters["todoId"] as String
        val todo = todoRepo.find(todoId) ?: return@get returnNotFound(call, "Todo")

        val taskId = call.parameters["taskId"] as String
        val task = todo.tasks.find {it.id == taskId} ?: return@get returnNotFound(call, "Task")

        call.respondText(Json.encodeToString(task), ContentType.Application.Json, HttpStatusCode.OK)
    }
}

private fun Route.deleteTaskRoute() {
    delete("/todos/{todoId}/tasks/{taskId}") {
        val todoId = call.parameters["todoId"] as String
        val todo = todoRepo.find(todoId) ?: return@delete returnNotFound(call, "Todo")

        val taskId = call.parameters["taskId"] as String
        todoRepo.deleteTask(todoId, taskId)
        call.respondText("", ContentType.Application.Json, HttpStatusCode.OK)
    }
}
