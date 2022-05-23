package com.example

import com.example.models.ApiResponse
import io.ktor.http.*
import io.ktor.application.*
import kotlin.test.*
import io.ktor.server.testing.*
import com.example.repository.HeroRepository
import com.example.repository.NEXT_PAGE_KEY
import com.example.repository.PREVIOUS_PAGE_KEY
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject

class ApplicationTest {
    private val heroRepository: HeroRepository by inject(HeroRepository::class.java)
    @Test
    fun `access root endpoint, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                assertEquals(
                    expected = "Welcome to Boruto API!",
                    actual = response.content)
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access all heroes endpoint, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            val page = 1..5
            val heroes = listOf(
                heroRepository.page1,
                heroRepository.page2,
                heroRepository.page3,
                heroRepository.page4,
                heroRepository.page5
            )

            page.forEach{ page->
                handleRequest(HttpMethod.Get, "/boruto/heroes?page=$page").apply{
                    println("CURRENTPAGE: $page")
                    assertEquals(
                        expected = HttpStatusCode.OK,
                        actual = response.status()
                    )
                    val expected = ApiResponse(
                        success = true,
                        message = "OK",
                        prevPage = calculatePage(page)["prevPage"],
                        nextPage = calculatePage(page)["nextPage"],
                        heroes = heroes[page-1]
                    )
                    val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    println("PREVIOUSPAGE: ${calculatePage(page)["prevPage"]}")
                    println("NEXTPAGE: ${calculatePage(page)["nextPage"]}")
                    println("HEROES: ${heroes[page - 1]}")
                    assertEquals(
                        expected = expected,
                        actual = actual
                    )
                }
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access all heroes endpoint, query specific page, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=2").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                val expected = ApiResponse(
                    success = true,
                    message = "OK",
                    prevPage = 1,
                    nextPage = 3,
                    heroes = heroRepository.page2
                )
                println("EXPECTED: $expected")
                println("ACTUAL: $actual")
                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access all heroes endpoint, query non-exsiting page number, assert eror`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=16").apply {
                assertEquals(
                    expected = HttpStatusCode.NotFound,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                val expected = ApiResponse(
                    success = false,
                    message = "Heroes not found."
                )
                println("EXPECTED: $expected")
                println("ACTUAL: $actual")
                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access all heroes endpoint, query invalid page number, assert eror`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=invalid").apply {
                assertEquals(
                    expected = HttpStatusCode.BadRequest,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                val expected = ApiResponse(
                    success = false,
                    message = "Only numbers Allowed!"
                )
                println("EXPECTED: $expected")
                println("ACTUAL: $actual")
                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search endpoint, query hero name, assert single hero result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=sasuke").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes.size
                assertEquals(
                    expected = 1,
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search endpoint, query hero name, assert multiple hero result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=sa").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes.size
                assertEquals(
                    expected = 3,
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search endpoint, query an empty text, assert empty list as a result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes
                assertEquals(
                    expected = emptyList(),
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search endpoint, query non existing hero, assert empty list as a result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=Wolverine").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    .heroes
                assertEquals(
                    expected = emptyList(),
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access non-existing endpoint, assert not found`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/unknown").apply {
                assertEquals(
                    expected = HttpStatusCode.NotFound,
                    actual = response.status()
                )
                assertEquals(
                    expected = "Page not found.",
                    actual = response.content
                )
            }
        }
    }



    private fun calculatePage(page: Int): Map<String, Int?>{
        var prevPage: Int? = page
        var nextPage: Int? = page
        if (page in 1..4){
            nextPage = nextPage?.plus(1)
        }
        if (page in 2..5){
            prevPage = prevPage?.minus(1)
        }
        if (page == 1){
            prevPage = null
        }
        if (page == 5){
            nextPage = null
        }
        return mapOf(PREVIOUS_PAGE_KEY to prevPage , NEXT_PAGE_KEY to nextPage)
    }

}