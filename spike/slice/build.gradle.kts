// Pure-Kotlin logic for the vertical slice (QUI-019), kept out of the Android module on
// purpose: casting and span clipping are where the bugs live, and here they are testable
// on a desktop in seconds rather than on a device by ear.
dependencies {
    api(project(":core:index"))
}
