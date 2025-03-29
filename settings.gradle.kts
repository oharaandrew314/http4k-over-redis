plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.60.3"
}
rootProject.name = "http-over-redis"
include("core")
include("jedis")
include("redisson")
