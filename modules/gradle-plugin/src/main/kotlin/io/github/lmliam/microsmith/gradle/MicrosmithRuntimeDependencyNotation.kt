package io.github.lmliam.microsmith.gradle

internal object MicrosmithRuntimeDependencyNotation {
    private const val GROUP = "io.github.lmliam.microsmith"
    private const val ARTIFACT = "runtime-scripting"

    fun runtimeScripting(): String = "$GROUP:$ARTIFACT:${MicrosmithGradlePluginVersion.current}"
}
