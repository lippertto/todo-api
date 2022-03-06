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
    val client = HttpClient.newBuilder().build();
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/${path}"))
        .build();
    val response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body()!!
}

private fun post(json: String, path: String): String {
    val client = HttpClient.newBuilder().build();
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/${path}"))
        .header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
    val response = client.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body()!!
}

private fun put(json: String, path: String): String {
    val client = HttpClient.newBuilder().build();
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/${path}"))
        .header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
        .PUT(HttpRequest.BodyPublishers.ofString(json))
        .build();
    val response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
        val response = post("""{"bad":"field"""", "/todos")
        val parsedResponse = Json.decodeFromString<ErrorResponse>(response)

        parsedResponse.error.code shouldBe "BadRequest"
    }

    "Can create a new todo" {
        val todo = Todo("any-id", "test", "test todo")
        val todoRequest = Json.encodeToString(todo)
        val response = post(todoRequest, "/todos")
        val parsedResponse = Json.decodeFromString<Todo>(response)

        parsedResponse.name shouldBe "test"
        parsedResponse.description shouldBe "test todo"
        parsedResponse.tasks shouldHaveSize 0
    }

    "Receives new id on create" {
        // GIVEN
        val todo = Todo("any-id", "test", "test todo")
        val todoRequest = Json.encodeToString(todo)

        // WHEN
        val createResponse = post(todoRequest, "/todos")
        val parsedCreationResponse = Json.decodeFromString<Todo>(createResponse)

        // THEN
        parsedCreationResponse.id shouldNotBe todo.id
    }

    "Retrieves created todo" {
        // GIVEN
        val todo = Todo("any-id", "test", "test todo")
        val todoRequest = Json.encodeToString(todo)

        // WHEN
        val createResponse = post(todoRequest, "/todos")
        val parsedCreationResponse = Json.decodeFromString<Todo>(createResponse)
        val getResponse = get("/todos/${parsedCreationResponse.id}")
        val parsedGetResponse = Json.decodeFromString<Todo>(getResponse)

        // THEN
        parsedCreationResponse shouldBe parsedGetResponse
    }

    "Can update todo" {
        // GIVEN
        val todo = Todo("any-id", "test", "test todo")
        val todoRequest = Json.encodeToString(todo)
        val createResponse = post(todoRequest, "/todos")
        val parsedCreationResponse = Json.decodeFromString<Todo>(createResponse)

        val updateTodo = todo.copy(description = "new-description")

        // WHEN
        val updateResponse = put(
            Json.encodeToString(updateTodo), "/todos/${parsedCreationResponse.id}"
        )
        val parsedUpdateResponse = Json.decodeFromString<Todo>(updateResponse)

        // THEN
        parsedUpdateResponse.description shouldBe "new-description"

        // AND
        val getResponse = get("/todos/${parsedCreationResponse.id}")
        val parsedGetResponse = Json.decodeFromString<Todo>(getResponse)
        parsedGetResponse.description shouldBe "new-description"
    }
})