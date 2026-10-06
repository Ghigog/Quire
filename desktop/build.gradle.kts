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

/**
 * The engine, if it has been fetched.
 *
 * Not committed (CLAUDE.md §9): ~8 MB of native library, reproducible from a `/tools`
 * script. That means a fresh clone does not have it, and this module cannot compile without
 * it — so its presence decides whether the module takes part in the build at all.
 */
val engineApi = fileTree("libs") { include("*sherpa-onnx-v*.jar") }
val engineNative = fileTree("libs") { include("*native-lib*.jar") }
val haveEngine = engineApi.files.isNotEmpty()

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:epub"))
    implementation(project(":core:attribution"))
    implementation(project(":core:voice"))
    implementation(project(":core:tts"))
    implementation(project(":core:index"))

    // The sherpa-onnx JVM API and its natives for the host OS, fetched by
    // tools/fetch-sherpa-jvm.sh. runtimeOnly for the natives: nothing here compiles against
    // them, sherpa's own loader binds them at first use.
    if (haveEngine) {
        implementation(engineApi)
        runtimeOnly(engineNative)
    }
}

application {
    applicationName = "quire"
    mainClass.set("quire.desktop.MainKt")
}

if (!haveEngine) {
    logger.lifecycle(
        """

        :desktop is not built — no sherpa-onnx JVM jars in desktop/libs.
        The module cannot compile without the engine, so it steps out of the build rather
        than breaking `./gradlew test` for everyone who has not fetched it. Everything
        else still runs.

        To build and test it:  tools/fetch-sherpa-jvm.sh
        To use it:             tools/fetch-sherpa-jvm.sh && tools/fetch-tts-model.sh

        """.trimIndent(),
    )
    tasks.configureEach {
        if (name != "clean") enabled = false
    }
}

