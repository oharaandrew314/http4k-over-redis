package dev.andrewohara.rhttp

import redis.clients.jedis.JedisPubSub
import java.time.Clock
import java.time.Duration

internal fun JedisPubSub.waitForSubscribed(
    timeout: Duration,
    clock: Clock,
) = HttpOverRedisUtils.await(timeout, clock) {
    if (isSubscribed) {
        ping() // helps make certain the subscription is ready
    } else null
}