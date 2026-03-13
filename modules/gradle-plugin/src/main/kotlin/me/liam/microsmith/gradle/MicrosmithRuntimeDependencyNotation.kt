package me.liam.microsmith.gradle

internal object MicrosmithRuntimeDependencyNotation {
    fun runtimeScripting(): String = "me.liam.microsmith:runtime-scripting:${MicrosmithGradlePluginVersion.current}"
}
