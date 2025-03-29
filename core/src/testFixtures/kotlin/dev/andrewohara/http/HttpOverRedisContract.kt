package dev.andrewohara.http

import io.kotest.matchers.shouldBe
import org.http4k.config.Host
import kotlin.random.Random
import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.kotest.shouldHaveBody
import org.http4k.kotest.shouldHaveStatus
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach

@OptIn(ExperimentalStdlibApi::class)
abstract class HttpOverRedisContract {

    abstract fun getClient(): HttpHandler
    abstract fun getServer(host: Host): ServerConfig

    private val host = Host("app1")

    @BeforeEach
    fun setup() {
        routes(
            "/body" bind Method.POST to {
                val content = it.body.stream.readAllBytes()
                println("Server received ${content.toHexString()}")
                Response(Status.OK).body(MemoryBody(content))
            },
            "/hello/{name}" bind Method.GET to {
                Response(Status.OK).body(it.path("name")!!)
            }
        )
            .asServer(getServer(host))
            .also { it.start() }
    }

    private val httpClient by lazy {
        ClientFilters
            .SetHostFrom(Uri.of("http://${host.value}"))
            .then(getClient())
    }

    @Test
    fun `GET request with response body`() = repeat(10) {
        println("GET $it")
        val response = Request(Method.GET, "/hello/bob").let(httpClient)
        response shouldHaveStatus Status.OK
        response shouldHaveBody "bob"
    }

    @Test
    fun `POST request with large binary body`() = repeat(10) {
        println("POST $it")
        val content = Random(1337).nextBytes(8)
        println("Generated ${content.toHexString()}")

        val response = Request(Method.POST, "/body")
            .body(MemoryBody(content))
            .let(httpClient)

        response shouldHaveStatus Status.OK
        val received = response.body.stream.readAllBytes().toHexString()
        println("Got back $received")
        response.body.stream.readAllBytes().toHexString() shouldBe content.toHexString()
    }

    // test headers
}
