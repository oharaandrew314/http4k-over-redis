[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central Version](https://img.shields.io/maven-central/v/dev.andrewohara/http4k-over-redis)](https://central.sonatype.com/artifact/dev.andrewohara/http4k-over-redis)

# Http4k over Redis

Http4k adapters to perform HTTP over Redis

## Why?

Useful for containers in a cluster to talk to each other without having access to their HTTP address.
Instead, route to them using an ID of your choice over a shared redis server.

:warning: You will need some other method to share server IDs with others in the cluster

## Requirements

- Java 21
- Http4k 6

## Compatibility

|            | http4k-over-redis-jedis | httpk-over-redis-redisson | 
|------------|-------------------------|---------------------------|
| Redis      | :white_check_mark:      | :white_check_mark:        |
| Valkey     | :white_check_mark:      | :white_check_mark:        |
| jedis-mock | :white_check_mark:      | :x:                       |

## Quickstart

```kotlin
// build.gradle.kts

dependencies {
    implementation("dev.andrewohara:http4k-over-redis-jedis:_")
    implementation("org.http4k:http4k-format-moshi:_")
    implementation("com.github.fppt:jedis-mock:_")
}
```

```kotlin
// Example.kt

import dev.andrewohara.http.jedisMock
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
    // replace with URI to your own redis/valkey server
    val redis = JedisPool("redis://localhost:${jedisMock().bindPort}")

    // Start server A
    val hostA = Host("A")
    http(hostA)
        .asServer(JedisHttpServer(redis, hostA, Moshi))
        .start()
    
    // Start server B
    val hostB = Host("B")
    http(hostB)
        .asServer(JedisHttpServer(redis, hostB, Moshi))
        .start()

    val client = JedisHttpClient(redis, Moshi)

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