package dev.andrewohara.http

import io.kotest.matchers.shouldBe
import org.http4k.config.Host
import kotlin.random.Random
import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.kotest.shouldHaveBody
import org.http4k.kotest.shouldHaveHeader
import org.http4k.kotest.shouldHaveStatus
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.ServerConfig
import org.http4k.server.asServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest

@OptIn(ExperimentalStdlibApi::class)
abstract class HttpOverRedisContract {

    abstract fun getClient(): HttpHandler
    abstract fun getServer(host: Host): ServerConfig

    protected val random = Random(1337)
    private val host = Host("server_${random.nextBytes(4).toHexString()}")

    @BeforeEach
    fun setup() {
        routes(
            "/echo" bind {
                Response(Status.OK)
                    .body(it.body)
                    .headers(it.headers)
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

    @RepeatedTest(10)
    fun `GET request with response body`() = repeat(10) {
        val name = random.nextBytes(8).toHexString()
        val response = Request(Method.GET, "/hello/$name").let(httpClient)
        response shouldHaveStatus Status.OK
        response shouldHaveBody name
    }

    @RepeatedTest(10)
    fun `POST request with large binary body`() = repeat(10) {
        val content = random.nextBytes(8)
        val response = Request(Method.POST, "/echo")
            .body(MemoryBody(content))
            .let(httpClient)

        response shouldHaveStatus Status.OK
        response.body.stream.readAllBytes().toHexString() shouldBe content.toHexString()
    }

    @RepeatedTest(10)
    fun `GET with headers`()  = repeat(10) {
        val response = Request(Method.GET, "/echo")
            .header("foo", "bar")
            .header("params", "toll=troll;spam=ham")
            .let(httpClient)

        response shouldHaveStatus Status.OK
        response.shouldHaveHeader("foo", "bar")
        response.shouldHaveHeader("params", "toll=troll;spam=ham")
    }
}
