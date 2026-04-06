package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp

internal fun csharpAttribute(name: String, arguments: String? = null): CSharp.Attribute =
    CSharp.Attribute(name = name, arguments = arguments)

internal fun csharpParameter(
    type: String,
    name: String,
    modifiers: List<String> = emptyList(),
    attributes: List<CSharp.Attribute> = emptyList(),
    defaultValue: String? = null,
): CSharp.Parameter = CSharp.Parameter(
    type = type,
    name = name,
    modifiers = modifiers,
    attributes = attributes,
    defaultValue = defaultValue,
)

internal fun extensionParameter(type: String, name: String): CSharp.Parameter = csharpParameter(
    type = type,
    name = name,
    modifiers = listOf("this"),
)
