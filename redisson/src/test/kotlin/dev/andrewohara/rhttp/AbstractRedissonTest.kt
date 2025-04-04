package dev.andrewohara.rhttp

import dev.andrewohara.http.HttpOverRedisContract
import org.http4k.config.Host
import org.http4k.format.Moshi
import org.redisson.api.RedissonClient
import java.time.Duration

abstract class AbstractRedissonTest: HttpOverRedisContract() {

    abstract val redisson: RedissonClient

    override fun getClient() = RedissonHttpClient(
        redisson = redisson,
        random = random,
        json = Moshi,
        responseTimeout = Duration.ofSeconds(1)
    )

    override fun getServer(host: Host) = RedissonHttpServer(
        redisson = redisson,
        host = host,
        json = Moshi
    )
}