package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetPackageVersionScope : DotnetPackageVersionsScope {
    public fun version(version: String)

    override operator fun String.unaryPlus()
}
