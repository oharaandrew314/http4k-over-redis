package dev.andrewohara.rhttp

import dev.andrewohara.http.fakeJedis
import redis.clients.jedis.JedisPool

class FakeJedisTest: AbstractJedisTest() {
    override val jedis = JedisPool("localhost", fakeJedis().bindPort)
}