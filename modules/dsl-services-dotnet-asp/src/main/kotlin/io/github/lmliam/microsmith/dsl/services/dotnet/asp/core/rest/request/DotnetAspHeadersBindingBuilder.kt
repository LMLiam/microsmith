package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

internal class DotnetAspHeadersBindingBuilder(private val name: String) : DotnetAspHeadersBindingScope {
    private val headers = mutableListOf<DotnetAspHeaderField>()

    override fun header(name: String): DotnetAspHeaderField {
        val field = DotnetAspHeaderField(name = headerFieldName(name), headerName = name.trim())
        headers += field
        return field
    }

    fun build() = DotnetAspHeadersBinding(name = name, headers = headers.toList())

    private fun headerFieldName(headerName: String): String {
        val normalized = headerName.trim()
        require(normalized.isNotBlank()) {
            "ASP.NET header name cannot be blank."
        }

        val candidate =
            normalized
                .split('-', '_', ' ')
                .filter(String::isNotBlank)
                .mapIndexed { index, segment ->
                    val lower = segment.lowercase()
                    if (index == 0) {
                        lower
                    } else {
                        lower.replaceFirstChar(Char::uppercaseChar)
                    }
                }.joinToString("")

        return candidate.ifBlank {
            error("Unable to derive an ASP.NET header field name from '$headerName'.")
        }
    }
}
