// Plays the companion app's role: builds a dialogue index with the real core:index
// writer, so what ships to the device is what the service reads. Spike, not product —
// the companion app proper is QUI-025.
plugins { application }

dependencies {
    implementation(project(":core:index"))
    implementation(project(":spike:slice"))
    // Desktop SQLite. Spike-only and never shipped; the index it writes is what ships,
    // not the driver.
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
}

application {
    mainClass.set("quire.spike.indexer.MainKt")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}
