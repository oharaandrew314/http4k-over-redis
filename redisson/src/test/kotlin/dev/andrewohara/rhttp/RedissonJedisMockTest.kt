package dev.andrewohara.rhttp

import dev.andrewohara.http.jedisMock
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import kotlin.test.Ignore

@Ignore("doesn't work properly")
class RedissonJedisMockTest: AbstractRedissonTest() {

    override val redisson: RedissonClient = Config()
        .apply {
            val mock = jedisMock()
            useSingleServer().setAddress("redis://${mock.host}:${mock.bindPort}")
        }
        .let { Redisson.create(it) }
}