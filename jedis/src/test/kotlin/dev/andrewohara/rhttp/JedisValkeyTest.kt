package dev.andrewohara.rhttp

import dev.andrewohara.http.valkeyContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import redis.clients.jedis.JedisPool

@Testcontainers
class JedisValkeyTest: AbstractJedisTest() {

    companion object {
        @Container
        @JvmStatic
        val container = valkeyContainer()
    }

    override val jedis = JedisPool(container.redisHost, container.redisPort)
}