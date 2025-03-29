dependencies {
    api(project(":core"))
    api("redis.clients:jedis:_")
    implementation("com.github.ben-manes.caffeine:caffeine:_")
    implementation("io.github.oshai:kotlin-logging-jvm:_")

    testImplementation(Http4k.format.moshi)
    testImplementation(testFixtures(project(":core")))
    testImplementation("org.slf4j:slf4j-simple:_")
}