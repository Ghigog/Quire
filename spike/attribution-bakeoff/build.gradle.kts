plugins {
    kotlin("jvm") version "2.2.20"
    application
}

repositories { mavenCentral() }

dependencies {
    implementation("quire:attribution")
    implementation("quire:model")
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(21) }

application {
    mainClass.set("quire.bakeoff.MainKt")
    // Java 18+ takes stdout's encoding from the console, not from file.encoding, and this
    // prints em dashes and curly quotes straight out of the corpus.
    applicationDefaultJvmArgs = listOf(
        "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8", "-Xmx2g",
    )
}

tasks.test {
    useJUnitPlatform()
    testLogging { showStandardStreams = true }
}
