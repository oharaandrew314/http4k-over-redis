dependencies {
    api(project(":core"))
    api("redis.clients:jedis:_")

    testImplementation(Http4k.format.moshi)
    testImplementation(testFixtures(project(":core")))
}