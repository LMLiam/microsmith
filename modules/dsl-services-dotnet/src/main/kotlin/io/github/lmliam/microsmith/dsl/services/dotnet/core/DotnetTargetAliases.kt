package io.github.lmliam.microsmith.dsl.services.dotnet.core

/**
 * User-facing target aliases exposed directly on .NET DSL scopes.
 */
@Suppress("VariableNaming")
interface DotnetTargetAliases {
    val NET5: DotnetTarget
        get() = DotnetTarget.NET5

    val NET6: DotnetTarget
        get() = DotnetTarget.NET6

    val NET7: DotnetTarget
        get() = DotnetTarget.NET7

    val NET8: DotnetTarget
        get() = DotnetTarget.NET8

    val NET9: DotnetTarget
        get() = DotnetTarget.NET9

    val NET10: DotnetTarget
        get() = DotnetTarget.NET10
}
