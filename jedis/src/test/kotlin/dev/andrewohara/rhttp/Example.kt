package dev.andrewohara.rhttp

import com.github.fppt.jedismock.RedisServer
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
    // Start fake redis server (testing only)
    val redisServer = RedisServer.newRedisServer().start()

    // replace with URI to your own redis/valkey server
    val jedis = JedisPool("redis://localhost:${redisServer.bindPort}")

    // Start server A
    val hostA = Host("A")
    http(hostA)
        .asServer(JedisHttpServer(jedis, hostA, Moshi))
        .start()

    // Start server B
    val hostB = Host("B")
    http(hostB)
        .asServer(JedisHttpServer(jedis, hostB, Moshi))
        .start()

    val client = JedisHttpClient(jedis, Moshi)

    // Send a request to server A
    Request(Method.GET, "http://A/foo")
        .let(client)
        .also { println(it.bodyString()) } // A:/foo

    // Send a request to server B
    Request(Method.GET, "http://B/bar")
        .let(client)
        .also { println(it.bodyString()) } // B:/bar
}