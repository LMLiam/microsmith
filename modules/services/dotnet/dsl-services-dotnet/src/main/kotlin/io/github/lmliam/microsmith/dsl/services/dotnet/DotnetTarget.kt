package io.github.lmliam.microsmith.dsl.services.dotnet

/** Validated .NET target framework monikers for the DSL. */
@JvmInline
value class DotnetTarget(val moniker: String) {
    init {
        require(moniker in supportedMonikers) { "Unsupported .NET target framework moniker: '$moniker'." }
    }

    override fun toString(): String = moniker

    companion object {
        private val supportedMonikers =
            setOf(
                "net10.0",
                "net11.0",
            )

        val NET10 = DotnetTarget("net10.0")
        val NET11 = DotnetTarget("net11.0")

        fun of(moniker: String): DotnetTarget = DotnetTarget(moniker)
    }
}
