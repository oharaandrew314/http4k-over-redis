dependencies {
    api(Http4k.core)
    api(Http4k.format.core)

    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testFixturesApi("com.github.fppt:jedis-mock:_")
    testFixturesApi("org.testcontainers:testcontainers")
    testFixturesApi("org.testcontainers:junit-jupiter:_")
    testFixturesApi("com.redis:testcontainers-redis:_")
}
