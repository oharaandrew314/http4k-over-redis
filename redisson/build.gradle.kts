dependencies {
    api(project(":core"))
    api("org.redisson:redisson:_")
    implementation("com.github.ben-manes.caffeine:caffeine:_")

    testImplementation(Http4k.format.moshi)
    testImplementation(testFixtures(project(":core")))
}