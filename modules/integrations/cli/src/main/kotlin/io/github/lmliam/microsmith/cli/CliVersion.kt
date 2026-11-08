package io.github.lmliam.microsmith.cli

internal const val FALLBACK_CLI_VERSION = "dev"

internal fun resolveCliVersion(): String {
    val implementationVersion = MicrosmithCli::class.java.`package`?.implementationVersion
    return implementationVersion?.takeIf { it.isNotBlank() } ?: FALLBACK_CLI_VERSION
}
