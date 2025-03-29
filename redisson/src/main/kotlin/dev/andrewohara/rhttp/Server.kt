package dev.andrewohara.rhttp

import org.http4k.config.Host
import org.http4k.core.HttpHandler
import org.http4k.format.Json
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import org.redisson.api.RedissonClient

fun <NODE> HttpOverRedis.redissonServer(
    redisson: RedissonClient,
    host: Host,
    json: Json<NODE>
) = object: ServerConfig {
    override fun toServer(http: HttpHandler) = object: Http4kServer {
        private val requestTopic = redisson.getTopic(host.value)
        private var listenerId: Int? = null

        override fun port() = throw NotImplementedError("http-over-redis does not bind to a port")

        override fun start(): Http4kServer {
            println("$host start listening")
            listenerId = requestTopic.addListener(String::class.java) { _, msg ->
                val parsed = RedisHttpMessage.parse(msg, json)
                val response = http(parsed.toRequest())

                val responseMessage = RedisHttpMessage(response, parsed.clientId, parsed.requestId)
                redisson.getTopic(parsed.clientId).publish(responseMessage.toJson(json))
            }
            println("$host listening on $listenerId")

            return this
        }

        override fun stop(): Http4kServer {
            listenerId?.let {
                requestTopic.removeListener(it)
            }
            return this
        }
    }
}