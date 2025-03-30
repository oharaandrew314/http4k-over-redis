package dev.andrewohara.rhttp

import org.http4k.core.*
import org.http4k.format.Json
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.time.Clock
import java.time.Duration
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random


@OptIn(ExperimentalStdlibApi::class)
object JedisHttpClient {
    operator fun <NODE> invoke(
        pool: JedisPool,
        json: Json<NODE>,
        random: Random = Random,
        clock: Clock = Clock.systemUTC(),
        subscribeTimeout: Duration = Duration.ofSeconds(1),
        responseTimeout: Duration = Duration.ofSeconds(10),
    ): HttpHandler {
        val clientId = "client_${random.nextBytes(4).toHexString()}"
        val logger = KotlinLogging.logger(clientId)

        val responseCache = Caffeine.newBuilder()
            .expireAfterWrite(responseTimeout)
            .build<String, Response>()

        val subscription = object : JedisPubSub() {
            override fun onMessage(channel: String, message: String) {
                val parsed = RedisHttpMessage.parse(message, json)
                logger.debug { "Got response to ${parsed.requestId}" }
                responseCache.put(parsed.requestId, parsed.toResponse())
            }
        }

        Thread.startVirtualThread {
            pool.resource.use {
                try {
                    logger.debug { "Subscribe" }
                    it.subscribe(subscription, clientId)
                } catch (e: Throwable) {
                    logger.error(e) { "Error subscribing" }
                } finally {
                    subscription.unsubscribe()
                    logger.debug { "Unsubscribed" }
                }
            }
        }.also { it.name = "$clientId-subscription" }

        subscription.waitForSubscribed(subscribeTimeout, clock)

        logger.debug { "Started" }

        return { request ->
            val message = RedisHttpMessage(request, clientId, "request_${random.nextBytes(4).toHexString()}")

            pool.resource.use { jedis ->
                try {
                    jedis.publish(request.uri.host, message.toJson(json))
                    logger.debug { "Sent ${message.requestId} to ${request.uri.host}" }
                } catch (e: ClassCastException) {
                    logger.warn(e) { "Error sending ${message.requestId} to ${request.uri.host}" }
                    // ignore.  Was sent anyway
                }
            }

            val response = HttpOverRedisUtils.await(responseTimeout, clock) {
                responseCache.getIfPresent(message.requestId)
            }

            response ?: Response(Status.REQUEST_TIMEOUT)
        }
    }
}