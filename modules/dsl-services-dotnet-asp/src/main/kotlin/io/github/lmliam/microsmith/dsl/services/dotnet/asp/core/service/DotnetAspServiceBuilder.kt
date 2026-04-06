package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRestBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRestScope

internal class DotnetAspServiceBuilder : DotnetAspServiceScope {
    private var rest: DotnetAspRest? = null

    override fun rest(block: DotnetAspRestScope.() -> Unit) {
        val declaredRest = DotnetAspRestBuilder().apply(block).build()
        rest = rest?.merge(declaredRest) ?: declaredRest
    }

    fun build() = DotnetAspServiceExtension(rest = rest)
}
