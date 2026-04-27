package dev.andrewohara.rhttp

import dev.andrewohara.http.HttpOverRedisContract
import org.http4k.config.Host
import org.http4k.format.Moshi
import redis.clients.jedis.RedisClient
import java.time.Duration

abstract class AbstractJedisTest: HttpOverRedisContract() {

    abstract val client: RedisClient

    override fun getClient() = JedisHttpClient(
        client = client,
        json = Moshi,
        random = random,
        responseTimeout = Duration.ofSeconds(1)
    )

    override fun getServer(host: Host) = JedisHttpServer(
        client = client,
        host = host,
        json = Moshi
    )
}