package io.github.lmliam.microsmith.dsl.services.dotnet.packages.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetPackageReferencesScope {
    public fun version(version: String)

    public operator fun String.invoke(block: DotnetPackageReferencesScope.() -> Unit = {})

    public operator fun String.unaryPlus()
}
