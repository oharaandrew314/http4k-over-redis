package dev.andrewohara.rhttp

import io.github.oshai.kotlinlogging.KotlinLogging
import redis.clients.jedis.JedisPubSub
import redis.clients.jedis.RedisClient
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

// the subscribe call is blocking, so we need a thread to hold it
// any exceptions thrown in fn will be logged
fun RedisClient.subscribeNonBlocking(
    channel: String,
    fnTimeout: Duration,
    executor: ExecutorService,
    fn: (String) -> Unit
): Thread {
    val logger = KotlinLogging.logger { }

    val watchDogPool = Executors.newVirtualThreadPerTaskExecutor()
    val subscribedLatch = CountDownLatch(1)

    val pubSub = object : JedisPubSub() {
        override fun onSubscribe(channel: String?, subscribedChannels: Int) {
            subscribedLatch.countDown()
        }

        override fun onMessage(channel: String?, message: String) {
            executor.execute {
                try {
                    // Enforce a hard timeout on the execution of the handler
                    // This protects the pool from being saturated by hanging tasks
                    watchDogPool.submit { fn(message) }.get(fnTimeout.toMillis(), TimeUnit.MILLISECONDS)
                } catch (e: TimeoutException) {
                    logger.warn(e) { "Handler for $channel timed out after $fnTimeout" }
                } catch (e: Exception) {
                    logger.error(e) { "Error processing message from $channel" }
                }
            }
        }
    }

    return Thread.ofVirtual().name("$channel-subscription").start {
        // if there's a network blip, we want to ensure we reacquire the subscription
        while (!Thread.currentThread().isInterrupted) {
            try {
                subscribe(pubSub, channel)
            } catch (_: InterruptedException) {
                pubSub.unsubscribe()
                break
            } catch (e: Exception) {
                logger.error(e) { "Error subscribing to $channel" }
                Thread.sleep(Duration.ofSeconds(1))
            }
        }
    }.also {
        // we need to wait for the subscription to complete; or else the server will be useless
        if (!subscribedLatch.await(5, TimeUnit.SECONDS)) {
            error("Failed to subscribe to $channel")
        } else {
            logger.trace { "Subscribed to $channel" }
        }
    }
}

fun boundedThreadPool(
    minThreads: Int,
    maxThreads: Int,
    queueSize: Int,
    threadNamePrefix: String,
    threadTtl: Duration = Duration.ofMinutes(1),
    withBackPressure: Boolean = false
) = ThreadPoolExecutor(
    minThreads, maxThreads,
    threadTtl.toMillis(), TimeUnit.MILLISECONDS,
    LinkedBlockingQueue(queueSize),
    Thread.ofVirtual().name("$threadNamePrefix-", 0).factory(),
    if (withBackPressure) ThreadPoolExecutor.CallerRunsPolicy() else ThreadPoolExecutor.DiscardOldestPolicy()
)