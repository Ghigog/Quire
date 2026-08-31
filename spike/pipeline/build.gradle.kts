plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("quire:attribution")
    implementation("quire:epub")
    implementation("org.jsoup:jsoup:1.21.1")
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(21) }

application {
    mainClass.set("quire.spike.MainKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}
