plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") apply false
    `java-test-fixtures`
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-test-fixtures")
    apply(plugin = "com.vanniktech.maven.publish")

    dependencies {
        api(platform(Http4k.bom))
        api(platform("org.testcontainers:testcontainers-bom:_"))

        testImplementation(kotlin("test"))

        testFixturesApi(Http4k.testing.kotest)
    }

    kotlin {
        jvmToolchain(21)
    }

    tasks.test {
        useJUnitPlatform()
    }
}