plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish")
    `java-test-fixtures`
}

allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "kotlin")
    apply(plugin = "java-test-fixtures")

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
