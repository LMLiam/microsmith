package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

internal class DotnetAspResponseHeadersBuilder : DotnetAspResponseHeadersScope {
    private val headers = mutableListOf<DotnetAspResponseHeader>()

    override fun header(name: String): DotnetAspResponseHeader {
        val header = DotnetAspResponseHeader(name.trim())
        headers += header
        return header
    }

    fun build() = headers.toList()
}
