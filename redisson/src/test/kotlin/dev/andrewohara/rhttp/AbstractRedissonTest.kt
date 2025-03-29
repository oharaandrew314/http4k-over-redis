package dev.andrewohara.rhttp

import dev.andrewohara.http.HttpOverRedisContract
import org.http4k.config.Host
import org.http4k.format.Moshi
import org.redisson.api.RedissonClient
import java.time.Duration

abstract class AbstractRedissonTest: HttpOverRedisContract() {

    abstract val redisson: RedissonClient

    override fun getClient() = HttpOverRedis.redissonClient(
        redisson = redisson,
        json = Moshi,
        responseTimeout = Duration.ofMillis(500)
    )

    override fun getServer(host: Host) = HttpOverRedis.redissonServer(
        redisson = redisson,
        host = host,
        json = Moshi
    )
}