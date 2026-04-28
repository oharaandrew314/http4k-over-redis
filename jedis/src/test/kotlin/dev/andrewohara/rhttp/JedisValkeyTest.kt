package dev.andrewohara.rhttp

import dev.andrewohara.http.valkeyContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import redis.clients.jedis.RedisClient

@Testcontainers
class JedisValkeyTest: AbstractJedisTest() {

    companion object {
        @Container
        @JvmStatic
        val container = valkeyContainer()
    }

    override val client: RedisClient = RedisClient.create(container.redisHost, container.redisPort)
}