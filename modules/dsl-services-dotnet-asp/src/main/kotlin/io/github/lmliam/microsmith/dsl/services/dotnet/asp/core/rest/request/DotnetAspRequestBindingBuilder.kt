package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

internal class DotnetAspRequestBindingBuilder(
    private val name: String,
) : DotnetAspRequestFieldSetBuilder("binding '$name'"), DotnetAspRequestBindingScope {

    fun build() = DotnetAspRequestBinding(name = name, fields = buildFields())
}
