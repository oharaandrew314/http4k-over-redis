package dev.andrewohara.rhttp

import dev.andrewohara.http.fakeJedis
import org.redisson.Redisson
import org.redisson.config.Config

class FakeJedisTest: AbstractRedissonTest() {

    override val redisson = Config()
        .apply { useSingleServer().setAddress("redis://localhost:${fakeJedis().bindPort}") }
        .let { Redisson.create(it) }
}