package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.DotnetAspRestBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.DotnetAspRestScope

internal class DotnetAspServiceBuilder : DotnetAspServiceScope {
    private var rest: DotnetAspRest? = null

    override fun rest(block: DotnetAspRestScope.() -> Unit) {
        val declaredRest = DotnetAspRestBuilder().apply(block).build()
        rest = rest?.merge(declaredRest) ?: declaredRest
    }

    fun build() = DotnetAspServiceExtension(rest = rest)
}
