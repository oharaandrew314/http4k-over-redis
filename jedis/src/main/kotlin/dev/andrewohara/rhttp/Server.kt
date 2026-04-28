package dev.andrewohara.rhttp

import io.github.oshai.kotlinlogging.KotlinLogging
import org.http4k.core.HttpHandler
import org.http4k.format.Json
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import redis.clients.jedis.RedisClient
import java.time.Clock
import java.time.Duration
import java.util.concurrent.ExecutorService

private const val CPU_THREADS_MULTIPLIER = 50
private const val THREADS_BUFFER_MULTIPLIER = 4

object JedisHttpServer {
    operator fun <NODE> invoke(
        client: RedisClient,
        hostId: String,
        json: Json<NODE>,
        clock: Clock = Clock.systemUTC(),
        maxThreads: Int = Runtime.getRuntime().availableProcessors() * CPU_THREADS_MULTIPLIER,
        bufferSize: Int = maxThreads * THREADS_BUFFER_MULTIPLIER,
        subscribeTimeout: Duration = Duration.ofSeconds(1)
    ) = object : ServerConfig {

        private var pollThread: Thread? = null
        private val logger = KotlinLogging.logger(hostId)
        private var handlerPool: ExecutorService? = null

        override fun toServer(http: HttpHandler) = object : Http4kServer {

            override fun port() = -1

            override fun start(): Http4kServer {
                if (pollThread != null) error("Already started")

                pollThread = client.subscribeNonBlocking(
                    channel = hostId,
                    fnTimeout = subscribeTimeout,
                    executor = boundedThreadPool(
                        minThreads = 1,
                        maxThreads = maxThreads,
                        queueSize = bufferSize,
                        threadNamePrefix = "$hostId-handler",
                        withBackPressure = false // best-effort processing
                    ).also { handlerPool = it },
                ) fn@{ requestBlob ->
                    try {
                        val parsed = RedisHttpMessage.parse(requestBlob, json)
                        val response = http(parsed.toRequest())
                        logger.trace { "Received ${parsed.requestId} from ${parsed.clientId}" }

                        val responseMessage = RedisHttpMessage(
                            response = response,
                            clientId = parsed.clientId,
                            requestId = parsed.requestId
                        )

                        try {
                            client.publish(parsed.clientId, responseMessage.toJson(json))
                            logger.trace { "Sent response for ${parsed.requestId} to ${parsed.clientId}" }
                        } catch (e: ClassCastException) {
                            logger.warn(e) { "Error sending response for ${parsed.requestId} to ${parsed.clientId}" }
                            // ignore.  Was sent anyway
                        }
                    } catch (e: Throwable) {
                        logger.error(e) { "Error!" }
                    }
                }

                return this
            }

            override fun stop(): Http4kServer {
                pollThread?.also {
                    it.interrupt()
                    it.join(Duration.ofSeconds(5))
                    pollThread = null
                } ?: error("Not started")

                handlerPool?.shutdown()
                handlerPool = null

                return this
            }
        }
    }
}