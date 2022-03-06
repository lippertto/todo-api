package dev.lippertto.todo

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.*

class EntitiesTest: StringSpec({
    "Can decode json" {
        val json = """{"id":"any-id","name":"any-name","description":"any-description"}"""
        val decoded = Json.decodeFromString<Todo>(json)

        decoded.id shouldBe "any-id"
        decoded.name shouldBe "any-name"
        decoded.description shouldBe "any-description"
    }
})