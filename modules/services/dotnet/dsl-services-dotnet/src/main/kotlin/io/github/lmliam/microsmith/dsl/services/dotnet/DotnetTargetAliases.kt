package io.github.lmliam.microsmith.dsl.services.dotnet

/** User-facing target aliases exposed directly on .NET DSL scopes. */
@Suppress("VariableNaming", "PropertyName")
interface DotnetTargetAliases {
    val NET10: DotnetTarget
        get() = DotnetTarget.NET10

    val NET11: DotnetTarget
        get() = DotnetTarget.NET11
}
