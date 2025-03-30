package dev.andrewohara.rhttp

import dev.andrewohara.http.jedisMock
import org.http4k.config.Host
import org.http4k.core.*
import org.http4k.format.Moshi
import org.http4k.server.asServer
import redis.clients.jedis.JedisPool

// create a server handler that responds with its own host and the request path
private fun http(host: Host): HttpHandler = {
    Response(Status.OK).body("${host.value}:${it.uri.path}")
}

fun main() {
    // replace with URI to your own redis/valkey server
    val redis = JedisPool("redis://localhost:${jedisMock().bindPort}")

    // Start server A
    val hostA = Host("A")
    http(hostA)
        .asServer(JedisHttpServer(redis, hostA, Moshi))
        .start()

    // Start server B
    val hostB = Host("B")
    http(hostB)
        .asServer(JedisHttpServer(redis, hostB, Moshi))
        .start()

    val client = JedisHttpClient(redis, Moshi)

    // Send a request to server A
    Request(Method.GET, "http://A/foo")
        .let(client)
        .also { println(it.bodyString()) } // A:/foo

    // Send a request to server B
    Request(Method.GET, "http://B/bar")
        .let(client)
        .also { println(it.bodyString()) } // B:/bar
}