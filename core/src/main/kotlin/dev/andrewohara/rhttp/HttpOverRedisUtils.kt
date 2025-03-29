package dev.andrewohara.rhttp

import java.time.Clock
import java.time.Duration

object HttpOverRedisUtils {
    fun <Resp: Any> await(
        timeout: Duration,
        clock: Clock,
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
}