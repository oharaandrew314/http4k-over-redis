dependencies {
    api(project(":core"))
    api("org.redisson:redisson:_")

    testImplementation(Http4k.format.moshi)
    testImplementation(testFixtures(project(":core")))
}