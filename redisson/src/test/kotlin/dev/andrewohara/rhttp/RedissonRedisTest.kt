package dev.andrewohara.rhttp

import dev.andrewohara.http.redisContainer
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class RedissonRedisTest: AbstractRedissonTest() {

    companion object {
        @Container
        @JvmStatic
        val container = redisContainer()
    }

    override val redisson: RedissonClient = Config()
        .apply { useSingleServer().setAddress("redis://${container.redisHost}:${container.redisPort}") }
        .let { Redisson.create(it) }
}