package dev.andrewohara.rhttp

import org.http4k.core.*
import org.http4k.format.Json
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun <NODE> HttpOverRedis.jedisClient(
    pool: JedisPool,
    json: Json<NODE>,
    subscribeTimeout: Duration = Duration.ofSeconds(1),
    responseTimeout: Duration = Duration.ofSeconds(10)
): HttpHandler {
    val clientId = UUID.randomUUID().toString()

    // TODO caffeine?
    val responseCache = ConcurrentHashMap<String, Response>()

    val subscription = object: JedisPubSub() {
        override fun onMessage(channel: String, message: String) {
            val parsed = RedisHttpMessage.parse(message, json)
            responseCache[parsed.requestId] = parsed.toResponse()
        }
    }

    Thread.startVirtualThread {
        pool.resource.use {
            try {
                it.subscribe(subscription, clientId)
            } finally {
                subscription.unsubscribe()
            }
        }
    }

    await(subscribeTimeout) {
        subscription.isSubscribed
    }

    return { request ->
        val message = RedisHttpMessage(request, clientId)

        pool.resource.use { jedis ->
            try {
                jedis.publish(request.uri.host, message.toJson(json))
            } catch (e: ClassCastException) {
                // ignore.  Was sent anyway
            }
        }

        val response = awaitNotNull(responseTimeout) {
            responseCache[message.requestId]
        }

        response ?: Response(Status.REQUEST_TIMEOUT)
    }
}