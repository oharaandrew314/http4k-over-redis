package dev.andrewohara.rhttp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.config.Host
import org.http4k.core.HttpHandler
import org.http4k.format.Json
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.redisson.api.RedissonClient

object RedissonHttpServer {
    operator fun <NODE> invoke(
        redisson: RedissonClient,
        host: Host,
        json: Json<NODE>
    ) = object : ServerConfig {
        override fun toServer(http: HttpHandler) = object : Http4kServer {

            private val topic = redisson.getTopic(host.value)
            private val logger = KotlinLogging.logger(host.value)
            private var listenerId: Int? = null

            override fun port() = throw NotImplementedError("http-over-redis does not bind to a port")

            override fun start(): Http4kServer {
                listenerId = topic.addListener(String::class.java) { _, msg ->
                    val parsed = RedisHttpMessage.parse(msg, json)
                    logger.debug { "Received ${parsed.requestId} from ${parsed.clientId}" }

                    val response = http(parsed.toRequest())
                    val responseMessage = RedisHttpMessage(response, parsed.clientId, parsed.requestId)

                    redisson.getTopic(parsed.clientId).publish(responseMessage.toJson(json))
                    logger.debug { "Sent response for ${parsed.requestId} to ${parsed.clientId}" }
                }

                logger.debug { topic.countSubscribers() }

                logger.debug { "Started" }

                return this
            }

            override fun stop(): Http4kServer {
                listenerId?.let {
                    topic.removeListener(it)
                }
                return this
            }
        }
    }
}