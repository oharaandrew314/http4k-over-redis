package dev.andrewohara.rhttp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.config.Host
import org.http4k.core.HttpHandler
import org.http4k.format.Json
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub
import java.time.Clock
import java.time.Duration

fun <NODE> HttpOverRedis.jedisServer(
    pool: JedisPool,
    host: Host,
    json: Json<NODE>,
    clock: Clock = Clock.systemUTC(),
    subscribeTimeout: Duration = Duration.ofSeconds(1)
) = object: ServerConfig {

    private var pollThread: Thread? = null
    private val logger = KotlinLogging.logger(host.value)

    override fun toServer(http: HttpHandler) = object: Http4kServer {

        override fun port() = throw NotImplementedError("http-over-redis does not bind to a port")

        override fun start(): Http4kServer {
            val subscription = object : JedisPubSub() {
                override fun onMessage(channel: String, message: String) {
                    try {
                        val parsed = RedisHttpMessage.parse(message, json)
                        val response = http(parsed.toRequest())
                        logger.debug { "Received ${parsed.requestId} from ${parsed.clientId}" }

                        val responseMessage = RedisHttpMessage(
                            response = response,
                            clientId = parsed.clientId,
                            requestId = parsed.requestId
                        )

                        pool.resource.use { jedis ->
                            try {
                                jedis.publish(parsed.clientId, responseMessage.toJson(json))
                                logger.debug { "Sent response for ${parsed.requestId} to ${parsed.clientId}" }
                            } catch (e: ClassCastException) {
                                logger.warn(e) { "Error sending response for ${parsed.requestId} to ${parsed.clientId}"}
                                // ignore.  Was sent anyway
                            }
                        }
                    } catch (e: Throwable) {
                        logger.error(e) { "Error!" }
                    }
                }
            }

            pollThread = Thread.startVirtualThread {
                try {
                    pool.resource.subscribe(subscription, host.value)
                } catch (e: Throwable) {
                    logger.error(e) { "Error subscribing" }
                } finally {
                    subscription.unsubscribe()
                }
            }
            subscription.waitForSubscribed(subscribeTimeout, clock)
            logger.debug { "Started" }

            return this
        }

        override fun stop(): Http4kServer {
            pollThread?.interrupt()
            return this
        }
    }
}