package dev.andrewohara.rhttp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.*
import org.http4k.format.Json
import java.time.Clock
import java.time.Duration
import redis.clients.jedis.RedisClient
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.random.Random

object JedisHttpClient {
    operator fun <NODE> invoke(
        client: RedisClient,
        json: Json<NODE>,
        random: Random = Random,
        clock: Clock = Clock.systemUTC(),
        subscribeTimeout: Duration = Duration.ofSeconds(1),
        responseTimeout: Duration = Duration.ofSeconds(10),
    ): HttpHandler {
        val logger = KotlinLogging.logger {  }
        val clientId = "client_${random.nextBytes(4).toHexString()}"

        val pendingRequests = ConcurrentHashMap<String, CompletableFuture<Response>>()

        // subscribe to responses (small worker since it's a lightweight operation)
        val responsePool = boundedThreadPool(
            minThreads = 1,
            maxThreads = 1,
            queueSize = 100,
            threadNamePrefix = "$clientId-handler",
            withBackPressure = true // slow down redis if we can't process immediately
        )

        val subscription = client.subscribeNonBlocking(clientId, subscribeTimeout, responsePool) { message ->
            try {
                val parsed = RedisHttpMessage.parse(message, json)
                pendingRequests.remove(parsed.requestId)?.complete(parsed.toResponse())
            } catch (e: Exception) {
                logger.error(e) { "[$clientId] Error parsing response: $message" }
            }
        }

        return object: HttpHandler, Closeable {
            override fun invoke(request: Request): Response {
                val message = RedisHttpMessage(request, clientId, "request_${random.nextBytes(4).toHexString()}")

                val future = CompletableFuture<Response>()
                pendingRequests[message.requestId] = future

                try {
                    client.publish(request.uri.host, message.toJson(json))
                    logger.trace { "[$clientId] Sent ${message.requestId} to ${request.uri.host}" }
                } catch (e: ClassCastException) {
                    logger.warn(e) { "[${clientId}] Error sending ${message.requestId} to ${request.uri.host}" }
                    // ignore.  Was sent anyway
                }

                return try {
                    future.get(responseTimeout.toMillis(), TimeUnit.MILLISECONDS)
                } catch (_: TimeoutException) {
                    Response(Status.CLIENT_TIMEOUT)
                } finally {
                    pendingRequests -= message.requestId
                }
            }

            override fun close() {
                subscription.interrupt()
                responsePool.shutdown()
            }
        }
    }
}