package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetPackageVersionScope : DotnetPackageVersionsScope {
    fun version(version: String)

    override operator fun String.unaryPlus()
}
