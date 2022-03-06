package dev.lippertto.todo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class MainKtTest : StringSpec({
    "can create a todo" {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Post, "/todos") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(Json.encodeToString(Todo("any-id", "any-name", "any-description")))
            }
                .apply {
                    val json = Json.decodeFromString<Todo>(response.content!!)
                    json.name shouldBe "any-name"
                    json.description shouldBe "any-description"
                }
        }
    }

    "Gets error response on bad request" {
        withTestApplication(Application::module) {
            handleRequest(HttpMethod.Post, "/todos") {
                addHeader(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody("""{"invalid":"json"""")
            }
                .apply {
                    val json = Json.decodeFromString<ErrorResponse>(response.content!!)

                    response.status() shouldBe HttpStatusCode.BadRequest
                    json.error.code shouldBe "BadRequest"
                }
        }
    }
})