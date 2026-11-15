package io.github.lmliam.microsmith.dsl.services.dotnet

/** Validated .NET target framework monikers for the DSL. */
@JvmInline
public value class DotnetTarget(public val moniker: String) {
    init {
        require(moniker in supportedMonikers) { "Unsupported .NET target framework moniker: '$moniker'." }
    }

    override fun toString(): String = moniker

    public companion object {
        private val supportedMonikers =
            setOf(
                "net10.0",
                "net11.0",
            )

        public val NET10: DotnetTarget = DotnetTarget("net10.0")
        public val NET11: DotnetTarget = DotnetTarget("net11.0")

        public fun of(moniker: String): DotnetTarget = DotnetTarget(moniker)
    }
}
