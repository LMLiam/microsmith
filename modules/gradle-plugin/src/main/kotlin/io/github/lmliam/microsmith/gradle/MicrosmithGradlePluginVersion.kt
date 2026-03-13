package io.github.lmliam.microsmith.gradle

internal object MicrosmithGradlePluginVersion {
    val current: String =
        checkNotNull(MicrosmithGradlePluginVersion::class.java.getResourceAsStream(VERSION_RESOURCE_PATH)) {
            "Microsmith Gradle plugin version resource '$VERSION_RESOURCE_PATH' was not found."
        }.bufferedReader().use { reader ->
            reader.readText().trim()
        }

    private const val VERSION_RESOURCE_PATH = "/META-INF/microsmith/gradle-plugin-version.txt"
}
