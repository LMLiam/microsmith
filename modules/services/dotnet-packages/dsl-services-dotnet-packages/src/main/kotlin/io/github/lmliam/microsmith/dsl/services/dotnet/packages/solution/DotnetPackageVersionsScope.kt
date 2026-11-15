package io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetPackageVersionsScope {
    public operator fun String.invoke(block: DotnetPackageVersionScope.() -> Unit = {})

    public operator fun String.unaryPlus()
}
