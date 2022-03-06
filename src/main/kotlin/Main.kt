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

private suspend fun returnNotFound (call: ApplicationCall) {
    call.respondText(
        Json.encodeToString(
            ErrorResponse(ErrorObject("NotFound", "Could not find entity with given id"))
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
        get("/todos/{todoId}") {
            val todoId = call.parameters["todoId"] as String
            val todo = todoRepo.find(todoId) ?: return@get returnNotFound(call)

            call.respondText(Json.encodeToString(todo), ContentType.Application.Json, HttpStatusCode.OK)
        }

        put("/todos/{todoId}") {
            val todoId = call.parameters["todoId"] as String
            todoRepo.find(todoId) ?: return@put returnNotFound(call)

            val todo = call.receive<Todo>()

            val updatedTodo = todoRepo.update(todoId, todo)
            call.respondText(Json.encodeToString(updatedTodo), ContentType.Application.Json, HttpStatusCode.OK)
        }

        post("/todos") {
            val todo = call.receive<Todo>()

            val newTodo = todoRepo.put(todo)

            call.respondText(Json.encodeToString(newTodo), ContentType.Application.Json, HttpStatusCode.OK)
        }
    }
}