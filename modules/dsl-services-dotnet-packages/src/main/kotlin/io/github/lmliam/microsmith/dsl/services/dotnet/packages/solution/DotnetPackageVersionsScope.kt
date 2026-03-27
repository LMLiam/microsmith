package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetPackageVersionsScope {
    operator fun String.invoke(block: DotnetPackageVersionScope.() -> Unit = {})
}
