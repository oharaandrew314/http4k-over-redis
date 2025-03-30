package dev.andrewohara.http

import com.github.fppt.jedismock.RedisServer
import com.redis.testcontainers.RedisContainer
import org.testcontainers.utility.DockerImageName

fun jedisMock(): RedisServer = RedisServer.newRedisServer().start()
fun redisContainer() = RedisContainer(DockerImageName.parse("redis:7.4.2"))
fun valkeyContainer() = RedisContainer(DockerImageName.parse("valkey/valkey:8.0.2"))