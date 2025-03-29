package dev.andrewohara.rhttp

import org.http4k.config.Host
import org.http4k.core.HttpHandler
import org.http4k.format.Json
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPubSub

fun <NODE> HttpOverRedis.jedisServer(pool: JedisPool, host: Host, json: Json<NODE>) = object: ServerConfig {

    private var pollThread: Thread? = null
    private var subscription: JedisPubSub? = null

    override fun toServer(http: HttpHandler) = object: Http4kServer {
        override fun port() = -1

        override fun start(): Http4kServer {
            subscription = object : JedisPubSub() {
                override fun onMessage(channel: String, message: String) {
                    val parsed = RedisHttpMessage.parse(message, json)
                    val response = http(parsed.toRequest())

                    val responseMessage = RedisHttpMessage(response, parsed.clientId, parsed.requestId)
                    pool.resource.use { jedis ->
                        try {
                            jedis.publish(parsed.clientId, responseMessage.toJson(json))
                        } catch (e: ClassCastException) {
                            // ignore.  Was sent anyway
                        }
                    }
                }
            }

            pollThread = Thread.startVirtualThread {
                pool.resource.subscribe(subscription, host.value)
            }

            return this
        }

        override fun stop(): Http4kServer {
            pool.close()
            subscription?.unsubscribe()
            pollThread?.interrupt()
            return this
        }
    }
}