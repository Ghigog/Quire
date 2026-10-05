// QUI-046: the desktop app.
//
// The whole pipeline — EPUB in, multi-voice audio out — as one runnable JVM program, so a
// book can be heard without an Android build, an install or a device. That is the point of
// the module: every piece below already exists and is tested, and nothing had ever wired
// them together end to end on a machine a person actually sits at.
//
// Pure JVM by design, like `core:*` (CLAUDE.md §9). The one thing that is not is the
// sherpa-onnx engine, and even that is a jar on the classpath rather than anything this
// module has to own — see tools/fetch-sherpa-jvm.sh.
plugins {
    application
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:epub"))
    implementation(project(":core:attribution"))
    implementation(project(":core:voice"))
    implementation(project(":core:tts"))
    implementation(project(":core:index"))

    // The sherpa-onnx JVM API and its natives for the host OS, fetched by
    // tools/fetch-sherpa-jvm.sh. Not committed (CLAUDE.md §9). runtimeOnly for the natives:
    // nothing here compiles against them, sherpa's own loader binds them at first use.
    implementation(fileTree("libs") { include("*sherpa-onnx-v*.jar") })
    runtimeOnly(fileTree("libs") { include("*native-lib*.jar") })
}

application {
    applicationName = "quire"
    mainClass.set("quire.desktop.MainKt")
}

/**
 * Fail early with the fix, rather than at the first `OfflineTts` call with an
 * `UnsatisfiedLinkError` that names a library nobody recognises.
 */
val checkEngineJars by tasks.registering {
    group = "verification"
    description = "Fails with instructions if the sherpa-onnx JVM jars have not been fetched."
    doLast {
        val libs = file("libs")
        val fetched = libs.listFiles()?.map { it.name }.orEmpty()
        if (fetched.none { it.matches(Regex("sherpa-onnx-v.*\\.jar")) }) {
            throw GradleException(
                "sherpa-onnx JVM jars are missing from desktop/libs.\n" +
                    "Run: tools/fetch-sherpa-jvm.sh",
            )
        }
    }
}

tasks.named("compileKotlin") { dependsOn(checkEngineJars) }
