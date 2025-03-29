package dev.andrewohara.rhttp

import dev.andrewohara.http.jedisMock
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config

class RedissonJedisMockTest: AbstractRedissonTest() {

    override val redisson: RedissonClient = Config()
        .apply { useSingleServer().setAddress("redis://localhost:${jedisMock().bindPort}") }
        .let { Redisson.create(it) }
}