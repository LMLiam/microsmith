package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRestBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.DotnetAspRestScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service.mergeDotnetAspRest

internal class DotnetAspServiceBuilder : DotnetAspServiceScope {
    private var ports: DotnetAspPorts? = null
    private var rest: DotnetAspRest? = null

    override fun ports(block: DotnetAspPortsScope.() -> Unit) {
        val declaredPorts = DotnetAspPortsBuilder().apply(block).build()
        ports = ports?.let { mergeDotnetAspPorts(it, declaredPorts) } ?: declaredPorts
    }

    override fun rest(block: DotnetAspRestScope.() -> Unit) {
        val declaredRest = DotnetAspRestBuilder().apply(block).build()
        rest = rest?.let { mergeDotnetAspRest(it, declaredRest) } ?: declaredRest
    }

    fun build() = DotnetAspServiceExtension(ports = ports, rest = rest)
}
