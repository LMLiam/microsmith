package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

internal class DotnetAspPortsBuilder : DotnetAspPortsScope {
    private var http: Int? = null
    private var https: Int? = null

    override fun http(port: Int) {
        require(http == null) { "ASP.NET service already declares an explicit HTTP port." }
        http = port
    }

    override fun https(port: Int) {
        require(https == null) { "ASP.NET service already declares an explicit HTTPS port." }
        https = port
    }

    fun build(): DotnetAspPorts = DotnetAspPorts(http = http, https = https)
}
