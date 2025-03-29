package dev.andrewohara.rhttp

import dev.andrewohara.http.HttpOverRedisContract
import org.http4k.config.Host
import org.http4k.format.Moshi
import redis.clients.jedis.JedisPool
import java.time.Duration

abstract class AbstractJedisTest: HttpOverRedisContract() {

    abstract val jedis: JedisPool

    override fun getClient() = HttpOverRedis.jedisClient(
        pool = jedis,
        json = Moshi,
        responseTimeout = Duration.ofMillis(500)
    )

    override fun getServer(host: Host) = HttpOverRedis.jedisServer(
        pool = jedis,
        host = host,
        json = Moshi
    )
}