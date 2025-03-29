package dev.andrewohara.rhttp

import com.github.benmanes.caffeine.cache.Caffeine
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.format.Json
import org.redisson.api.RedissonClient
import java.time.Clock
import java.time.Duration
import kotlin.random.Random

@OptIn(ExperimentalStdlibApi::class)
fun <NODE> HttpOverRedis.redissonClient(
    redisson: RedissonClient,
    json: Json<NODE>,
    clock: Clock = Clock.systemUTC(),
    random: Random = Random,
    responseTimeout: Duration = Duration.ofSeconds(10)
): HttpHandler {
    val clientId = "client_${random.nextBytes(4).toHexString()}"

    val responseCache = Caffeine.newBuilder()
        .expireAfterWrite(responseTimeout)
        .build<String, Response>()

    println("client start listening")
    redisson.getTopic(clientId).addListener(String::class.java) { _, msg ->
        val parsed = RedisHttpMessage.parse(msg, json)
        responseCache.put(parsed.requestId, parsed.toResponse())
    }
    println("client listening")

    return { request ->
        val message = RedisHttpMessage(
            request = request,
            clientId = clientId,
            requestId = "request_${random.nextBytes(4).toHexString()}"
        )
        redisson.getTopic(request.uri.host).publish(message.toJson(json))

        val response = HttpOverRedisUtils.await(responseTimeout, clock) {
            responseCache.getIfPresent(message.requestId)
        }

        response ?: Response(Status.REQUEST_TIMEOUT)
    }
}