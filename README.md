[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

# Http4k over Redis

Http4k adapters to perform HTTP over Redis

## Why?

Useful for containers in a cluster to talk to each other without having access to their HTTP address.
Instead, route to them using an ID of your choice over a shared redis server.

:warning: You will need some other method to share server IDs with others in the cluster

## Requirements

- Java 21
- Http4k 6

## Modules

### Jedis

[![Maven Central Version](https://img.shields.io/maven-central/v/dev.andrewohara/http4k-over-redis-jedis)](https://central.sonatype.com/artifact/dev.andrewohara/http4k-over-redis-jedis)

| Server    | Redis              | Valkey             | [fppt/jedis-mock](https://github.com/fppt/jedis-mock) |
|-----------|--------------------|--------------------|-------------------------------------------------------|
| Supported | :white_check_mark: | :white_check_mark: | :white_check_mark:                                    |

### Redisson

[![Maven Central Version](https://img.shields.io/maven-central/v/dev.andrewohara/http4k-over-redis-redisson)](https://central.sonatype.com/artifact/dev.andrewohara/http4k-over-redis-redisson)

| Server    | Redis              | Valkey             | [fppt/jedis-mock](https://github.com/fppt/jedis-mock) |
|-----------|--------------------|--------------------|-------------------------------------------------------|
| Supported | :white_check_mark: | :white_check_mark: | :x:                                                   |

## Quickstart

Use Jedis running against a jedis-mock server

```kotlin
// build.gradle.kts

dependencies {
    implementation("dev.andrewohara:http4k-over-redis-jedis:_")
    implementation("com.github.fppt:jedis-mock:_")

    // Any Json implementation from http4k-format will work
    implementation("org.http4k:http4k-format-moshi:_")
}
```

```kotlin
// Example.kt

import com.github.fppt.jedismock.RedisServer
import org.http4k.config.Host
import org.http4k.core.*
import org.http4k.format.Moshi
import org.http4k.server.asServer
import redis.clients.jedis.JedisPool

// create a server handler that responds with its own host and the request path
private fun http(host: Host): HttpHandler = {
    Response(Status.OK).body("${host.value}:${it.uri.path}")
}

fun main() {
    // Start fake redis server (testing only)
    val redisServer = RedisServer.newRedisServer().start()

    // replace with URI to your own redis/valkey server
    val jedis = JedisPool("redis://localhost:${redisServer.bindPort}")

    // Start server A
    val hostA = Host("A")
    http(hostA)
        .asServer(JedisHttpServer(jedis, hostA, Moshi))
        .start()

    // Start server B
    val hostB = Host("B")
    http(hostB)
        .asServer(JedisHttpServer(jedis, hostB, Moshi))
        .start()

    val client = JedisHttpClient(jedis, Moshi)

    // Send a request to server A
    Request(Method.GET, "http://A/foo")
        .let(client)
        .also { println(it.bodyString()) } // A:/foo

    // Send a request to server B
    Request(Method.GET, "http://B/bar")
        .let(client)
        .also { println(it.bodyString()) } // B:/bar
}
```