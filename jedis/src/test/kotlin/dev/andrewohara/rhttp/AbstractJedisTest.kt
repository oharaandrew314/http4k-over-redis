package dev.andrewohara.rhttp

import dev.andrewohara.http.HttpOverRedisContract
import org.http4k.config.Host
import org.http4k.format.Moshi
import redis.clients.jedis.JedisPool
import java.time.Duration

abstract class AbstractJedisTest: HttpOverRedisContract() {

    abstract val jedis: JedisPool

    override fun getClient() = JedisHttpClient(
        pool = jedis,
        json = Moshi,
        random = random,
        responseTimeout = Duration.ofSeconds(1)
    )

    override fun getServer(host: Host) = JedisHttpServer(
        pool = jedis,
        host = host,
        json = Moshi
    )
}