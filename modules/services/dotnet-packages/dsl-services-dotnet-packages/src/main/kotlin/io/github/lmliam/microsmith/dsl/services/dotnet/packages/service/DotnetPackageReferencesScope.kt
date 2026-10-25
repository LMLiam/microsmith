package io.github.lmliam.microsmith.dsl.services.dotnet.packages.service

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface DotnetPackageReferencesScope {
    fun version(version: String)

    operator fun String.invoke(block: DotnetPackageReferencesScope.() -> Unit = {})

    operator fun String.unaryPlus()
}
