package dev.andrewohara.rhttp

import dev.andrewohara.http.REDIS_PORT
import dev.andrewohara.http.redisContainer
import org.redisson.Redisson
import org.redisson.config.Config
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class ValkeyTest: AbstractRedissonTest() {

    companion object {
        @Container
        @JvmStatic
        val container = redisContainer()
    }

    override val redisson = Config()
        .apply { useSingleServer().setAddress("valkey://localhost:${container.getMappedPort(REDIS_PORT)}") }
        .let { Redisson.create(it) }
}