package dev.andrewohara.rhttp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Json
import org.redisson.api.RedissonClient
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import  java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.random.Random

object RedissonHttpClient {
    operator fun <NODE> invoke(
        redisson: RedissonClient,
        json: Json<NODE>,
        clock: Clock = Clock.systemUTC(),
        random: Random = Random,
        responseTimeout: Duration = Duration.ofSeconds(10)
    ): HttpHandler {
        val clientId = "client_${random.nextBytes(4).toHexString()}"
        val logger = KotlinLogging.logger(clientId)

        val pendingRequests = ConcurrentHashMap<String, CompletableFuture<Response>>()

        redisson.getTopic(clientId).addListener(String::class.java) { _, msg ->
            val parsed = RedisHttpMessage.parse(msg, json)
            logger.trace { "Got response to ${parsed.requestId}" }
            pendingRequests[parsed.requestId]?.complete(parsed.toResponse())
        }

        logger.trace { "Started" }

        return { request ->
            val message = RedisHttpMessage(
                request = request,
                clientId = clientId,
                requestId = "request_${random.nextBytes(4).toHexString()}"
            )

            val future = CompletableFuture<Response>()
            pendingRequests[message.requestId] = future

            redisson.getTopic(request.uri.host).publish(message.toJson(json))
            logger.trace { "Sent ${message.requestId} to ${request.uri.host}" }

            try {
                future.get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS)
            } catch(_: TimeoutException) {
                Response(Status.CLIENT_TIMEOUT)
            } finally {
                pendingRequests.remove(message.requestId)
            }
        }
    }
}