package dev.andrewohara.rhttp

import java.time.Clock
import java.time.Duration

fun await(
    timeout: Duration,
    clock: Clock = Clock.systemUTC(),
    sleepTime: Duration = Duration.ofMillis(10),
    cond: () -> Boolean
): Boolean {
    val start = clock.instant()
    while(Duration.between(start, clock.instant()) < timeout) {
        if (cond()) return true
        Thread.sleep(sleepTime)
    }

    return false
}

fun <Resp: Any> awaitNotNull(
    timeout: Duration,
    clock: Clock = Clock.systemUTC(),
    sleepTime: Duration = Duration.ofMillis(10),
    fn: () -> Resp?
): Resp? {
    val start = clock.instant()
    while(Duration.between(start, clock.instant()) < timeout) {
        fn()?.let { return it }
        Thread.sleep(sleepTime)
    }

    return null
}