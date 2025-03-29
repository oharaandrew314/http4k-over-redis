package dev.andrewohara.rhttp

import dev.andrewohara.http.REDIS_PORT
import dev.andrewohara.http.valkeyContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import redis.clients.jedis.JedisPool

@Testcontainers
class ValkeyTest: AbstractJedisTest() {

    companion object {
        @Container
        @JvmStatic
        val container = valkeyContainer()
    }

    override val jedis = JedisPool("localhost", container.getMappedPort(REDIS_PORT))
}