package dev.lippertto.todo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.TimeUnit


private fun get(path: String): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/${path}"))
        .GET()
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()!!
}

private fun delete(path: String): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/${path}"))
        .DELETE()
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()!!
}

private fun post(path: String, json: String): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/${path}"))
        .header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()!!
}

private fun put(path: String, json: String): String {
    val client = HttpClient.newBuilder().build()
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/${path}"))
        .header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        .PUT(HttpRequest.BodyPublishers.ofString(json))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    return response.body()!!
}

private fun runGradleApp(): Process = runGradle("run")
private fun runGradle(vararg args: String): Process {
    val gradlewPath =
        "/home/lippertto/Code/todo-api/gradlew"//System.getProperty("gradlew") ?: error("System property 'gradlew' should point to Gradle Wrapper file")
    val processArgs = listOf(gradlewPath, "-Dorg.gradle.logging.level=quiet", "--quiet") + args

    return ProcessBuilder(processArgs).start()
}


private lateinit var serverProcess: Process

class IntegrationTest : StringSpec({
    beforeSpec {
        serverProcess = runGradleApp()
        serverProcess.waitFor(8, TimeUnit.SECONDS)
    }
    afterSpec {
        runBlocking {
            serverProcess.destroy()
        }
    }

    "Returns 404 on unknown todo" {
        val response = get("/todos/1")
        val parsedResponse = Json.decodeFromString<ErrorResponse>(response)
        parsedResponse.error.code shouldBe "NotFound"
    }

    "Gets 400 on bad todo creation" {
        val response = post("/todos", """{"bad":"field"""")
        val parsedResponse = Json.decodeFromString<ErrorResponse>(response)
        parsedResponse.error.code shouldBe "BadRequest"
    }

    "Can create a new todo" {
        // GIVEN
        val response = post(
            "/todos", Json.encodeToString(
                Todo("any-id", "test", "test todo")
            )
        )

        // WHEN
        val parsedResponse = Json.decodeFromString<Todo>(response)

        // THEN
        parsedResponse.name shouldBe "test"
        parsedResponse.description shouldBe "test todo"
        parsedResponse.tasks shouldHaveSize 0
    }

    "Receives new id on create" {
        // GIVEN
        val todo = Json.encodeToString(Todo("any-id", "test", "test todo"))

        // WHEN
        val returnedTodo = Json.decodeFromString<Todo>(
            post("/todos", todo)
        )

        // THEN
        returnedTodo.id shouldNotBe "any-id"
    }

    "Retrieves created todo" {
        // GIVEN
        val todo = Json.encodeToString(Todo("any-id", "test", "test todo"))
        val createdTodo = Json.decodeFromString<Todo>(post("/todos", todo))

        // WHEN
        val parsedGetResponse = Json.decodeFromString<Todo>(
            get("/todos/${createdTodo.id}")
        )

        // THEN
        createdTodo shouldBe parsedGetResponse
    }

    "Can update todo" {
        // GIVEN
        val createdTodo = Json.decodeFromString<Todo>(
            post(
                "/todos",
                Json.encodeToString(Todo("any-id", "test", "test todo"))
            )
        )

        // WHEN
        val updatedTodo = Json.decodeFromString<Todo>(
            put(
                "/todos/${createdTodo.id}",
                Json.encodeToString(
                    Todo("any-id", "test", "new-description")
                )
            )
        )

        // THEN
        updatedTodo.description shouldBe "new-description"

        // AND
        val retrievedTodo = Json.decodeFromString<Todo>(
            get("/todos/${createdTodo.id}")
        )
        retrievedTodo.description shouldBe "new-description"
    }

    "deleted todo is deleted" {
        // GIVEN
        val createResponse = Json.decodeFromString<Todo>(
            post(
                "/todos",
                Json.encodeToString(Todo("any-id", "test", "test todo"))
            )
        )

        // WHEN
        delete("/todos/${createResponse.id}")

        // THEN
        Json.decodeFromString<ErrorResponse>(
            get(
                "/todos/${createResponse.id}"
            )
        ).error.code shouldBe "NotFound"
    }

    "Can add task" {
        // GIVEN
        val todoId = Json.decodeFromString<Todo>(
            post(
                "/todos",
                Json.encodeToString(Todo("any-id", "test", "test todo"))
            )
        ).id

        // WHEN
        val createdTask = Json.decodeFromString<Task>(
            post(
                "/todos/$todoId/tasks",
                Json.encodeToString(Task(id = "any-id", name = "any-name"))
            )
        )

        // THEN
        createdTask.name shouldBe "any-name"
    }

    "Can retrieve task" {
        // GIVEN
        val todoId = Json.decodeFromString<Todo>(
            post(
                "/todos",
                Json.encodeToString(Todo("any-id", "test", "test todo"))
            )
        ).id

        val taskId = Json.decodeFromString<Task>(
            post(
                "/todos/$todoId/tasks",
                Json.encodeToString(Task(id = "any-id", name = "any-name"))
            )
        ).id

        // WHEN
        val retrievedTask = Json.decodeFromString<Task>(
            get("/todos/$todoId/tasks/$taskId")
        )

        // THEN
        retrievedTask.name shouldBe "any-name"
    }

    "Can update task" {
        // GIVEN
        val todo = Todo("any-id", "test", "test todo")
        val todoId = Json.decodeFromString<Todo>(post("/todos", Json.encodeToString(todo))).id

        val taskId =
            Json.decodeFromString<Task>(
                post("/todos/$todoId/tasks", Json.encodeToString(Task(id = "any-id", name = "any-name")))
            ).id

        // WHEN
        val updatedTask =
            Json.decodeFromString<Task>(
                put("/todos/$todoId/tasks/$taskId", Json.encodeToString(Task(id = "any-id", name = "new-name")))
            )

        // THEN
        updatedTask.name shouldBe "new-name"

        // AND THEN
        Json.decodeFromString<Task>(
            get("/todos/$todoId/tasks/$taskId")
        ).name shouldBe "new-name"
    }

    "Can delete task" {
        // GIVEN
        val todo = Todo("any-id", "test", "test todo")
        val todoId = Json.decodeFromString<Todo>(post("/todos", Json.encodeToString(todo))).id

        val taskId =
            Json.decodeFromString<Task>(
                post("/todos/$todoId/tasks", Json.encodeToString(Task(id = "any-id", name = "any-name")))
            ).id

        // WHEN
        delete("/todos/$todoId/tasks/$taskId")


        // THEN
        Json.decodeFromString<ErrorResponse>(
            get("/todos/$todoId/tasks/$taskId")
        ).error.code shouldBe "NotFound"
    }
})