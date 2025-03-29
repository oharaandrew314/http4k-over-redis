package dev.andrewohara.rhttp

import dev.andrewohara.http.jedisMock
import redis.clients.jedis.JedisPool

class JedisJedisMockTest: AbstractJedisTest() {
    override val jedis = JedisPool("localhost", jedisMock().bindPort)
}