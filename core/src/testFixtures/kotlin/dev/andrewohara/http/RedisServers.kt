package dev.andrewohara.http

import com.github.fppt.jedismock.RedisServer
import com.redis.testcontainers.RedisContainer
import org.testcontainers.utility.DockerImageName

fun jedisMock() = RedisServer.newRedisServer().start().also { Thread.sleep(100) }
fun redisContainer() = RedisContainer(DockerImageName.parse("redis:7.4.2"))
fun valkeyContainer() = RedisContainer(DockerImageName.parse("valkey/valkey:8.0.2"))