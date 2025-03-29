package dev.andrewohara.rhttp

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Json
import org.redisson.api.RedissonClient
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun <NODE> HttpOverRedis.redissonClient(
    redisson: RedissonClient,
    json: Json<NODE>,
    responseTimeout: Duration = Duration.ofSeconds(10)
): HttpHandler {
    val clientId = UUID.randomUUID().toString()

    // TODO caffeine?
    val responseCache = ConcurrentHashMap<String, Response>()

    redisson.getTopic(clientId).addListener(String::class.java) { _, msg ->
        val parsed = RedisHttpMessage.parse(msg, json)
        responseCache[parsed.requestId] = parsed.toResponse()
    }

    return { request ->
        val message = RedisHttpMessage(request, clientId)
        redisson.getTopic(request.uri.host).publish(message.toJson(json))

        val response = awaitNotNull(responseTimeout) {
            responseCache[message.requestId]
        }

        response ?: Response(Status.REQUEST_TIMEOUT)
    }
}