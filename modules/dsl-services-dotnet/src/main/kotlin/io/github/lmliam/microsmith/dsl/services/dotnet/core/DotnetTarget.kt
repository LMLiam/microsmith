package io.github.lmliam.microsmith.dsl.services.dotnet.core

/**
 * Supported .NET target framework markers for the DSL.
 */
sealed class DotnetTarget(val moniker: String) {
    data object NET8 : DotnetTarget("net8.0")

    data object NET9 : DotnetTarget("net9.0")

    override fun toString(): String = moniker
}
