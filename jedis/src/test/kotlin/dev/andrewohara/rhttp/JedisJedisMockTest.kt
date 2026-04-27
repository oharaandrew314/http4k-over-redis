package dev.andrewohara.rhttp

import dev.andrewohara.http.jedisMock
import redis.clients.jedis.RedisClient

class JedisJedisMockTest: AbstractJedisTest() {
    override val client: RedisClient = RedisClient.create("localhost", jedisMock().bindPort)
}