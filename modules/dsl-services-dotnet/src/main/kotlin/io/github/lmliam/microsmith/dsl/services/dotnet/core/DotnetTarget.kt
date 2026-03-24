package io.github.lmliam.microsmith.dsl.services.dotnet.core

/**
 * Validated .NET target framework monikers for the DSL.
 */
@JvmInline
value class DotnetTarget(
    val moniker: String,
) {
    init {
        require(moniker in supportedMonikers) {
            "Unsupported .NET target framework moniker: '$moniker'."
        }
    }

    override fun toString(): String = moniker

    companion object {
        private val supportedMonikers = setOf(
            "netcoreapp1.0",
            "netcoreapp1.1",
            "netcoreapp2.0",
            "netcoreapp2.1",
            "netcoreapp2.2",
            "netcoreapp3.0",
            "netcoreapp3.1",
            "net5.0",
            "net6.0",
            "net7.0",
            "net8.0",
            "net9.0",
            "net10.0",
            "net11",
            "net20",
            "net35",
            "net40",
            "net403",
            "net45",
            "net451",
            "net452",
            "net46",
            "net461",
            "net462",
            "net47",
            "net471",
            "net472",
            "net48",
            "net481",
        )

        val NETCOREAPP1_0 = DotnetTarget("netcoreapp1.0")
        val NETCOREAPP1_1 = DotnetTarget("netcoreapp1.1")
        val NETCOREAPP2_0 = DotnetTarget("netcoreapp2.0")
        val NETCOREAPP2_1 = DotnetTarget("netcoreapp2.1")
        val NETCOREAPP2_2 = DotnetTarget("netcoreapp2.2")
        val NETCOREAPP3_0 = DotnetTarget("netcoreapp3.0")
        val NETCOREAPP3_1 = DotnetTarget("netcoreapp3.1")

        val NET5 = DotnetTarget("net5.0")
        val NET6 = DotnetTarget("net6.0")
        val NET7 = DotnetTarget("net7.0")
        val NET8 = DotnetTarget("net8.0")
        val NET9 = DotnetTarget("net9.0")
        val NET10 = DotnetTarget("net10.0")

        val NET11 = DotnetTarget("net11")
        val NET20 = DotnetTarget("net20")
        val NET35 = DotnetTarget("net35")
        val NET40 = DotnetTarget("net40")
        val NET403 = DotnetTarget("net403")
        val NET45 = DotnetTarget("net45")
        val NET451 = DotnetTarget("net451")
        val NET452 = DotnetTarget("net452")
        val NET46 = DotnetTarget("net46")
        val NET461 = DotnetTarget("net461")
        val NET462 = DotnetTarget("net462")
        val NET47 = DotnetTarget("net47")
        val NET471 = DotnetTarget("net471")
        val NET472 = DotnetTarget("net472")
        val NET48 = DotnetTarget("net48")
        val NET481 = DotnetTarget("net481")

        fun of(moniker: String): DotnetTarget = DotnetTarget(moniker)
    }
}
