package io.github.lmliam.microsmith.dsl.services.dotnet.core

/**
 * Validated .NET target framework monikers for the DSL.
 */
@JvmInline
value class DotnetTarget(val moniker: String) {
    init {
        require(moniker in supportedMonikers) {
            "Unsupported .NET target framework moniker: '$moniker'."
        }
    }

    override fun toString(): String = moniker

    companion object {
        private val supportedMonikers = setOf(
            "net5.0",
            "net6.0",
            "net7.0",
            "net8.0",
            "net9.0",
            "net10.0",
        )

        val NET5 = DotnetTarget("net5.0")
        val NET6 = DotnetTarget("net6.0")
        val NET7 = DotnetTarget("net7.0")
        val NET8 = DotnetTarget("net8.0")
        val NET9 = DotnetTarget("net9.0")
        val NET10 = DotnetTarget("net10.0")

        fun of(moniker: String): DotnetTarget = DotnetTarget(moniker)
    }
}
