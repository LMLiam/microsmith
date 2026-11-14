package io.github.lmliam.microsmith.dsl.services.dotnet

/** User-facing target aliases exposed directly on .NET DSL scopes. */
@Suppress("VariableNaming", "PropertyName", "DEPRECATION")
interface DotnetTargetAliases {
    @Deprecated(DEPRECATED_TARGET_MESSAGE, ReplaceWith("NET10"))
    val NET5: DotnetTarget
        get() = DotnetTarget.NET5

    @Deprecated(DEPRECATED_TARGET_MESSAGE, ReplaceWith("NET10"))
    val NET6: DotnetTarget
        get() = DotnetTarget.NET6

    @Deprecated(DEPRECATED_TARGET_MESSAGE, ReplaceWith("NET10"))
    val NET7: DotnetTarget
        get() = DotnetTarget.NET7

    @Deprecated(DEPRECATED_TARGET_MESSAGE, ReplaceWith("NET10"))
    val NET8: DotnetTarget
        get() = DotnetTarget.NET8

    @Deprecated(DEPRECATED_TARGET_MESSAGE, ReplaceWith("NET10"))
    val NET9: DotnetTarget
        get() = DotnetTarget.NET9

    val NET10: DotnetTarget
        get() = DotnetTarget.NET10

    val NET11: DotnetTarget
        get() = DotnetTarget.NET11
}
