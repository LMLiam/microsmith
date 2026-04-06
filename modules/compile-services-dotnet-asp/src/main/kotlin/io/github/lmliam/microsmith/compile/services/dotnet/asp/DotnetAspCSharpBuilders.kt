package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpFileBuilder

internal fun csharpAttribute(name: String, arguments: String? = null): CSharp.Attribute =
    CSharp.Attribute(name = name, arguments = arguments)

internal fun csharpParameter(
    type: CSharp.TypeRef,
    name: String,
    modifiers: List<CSharp.Modifier> = emptyList(),
    attributes: List<CSharp.Attribute> = emptyList(),
    defaultValue: String? = null,
): CSharp.Parameter = CSharp.Parameter(
    type = type,
    name = name,
    modifiers = modifiers,
    attributes = attributes,
    defaultValue = defaultValue,
)

internal fun csharpParameter(
    type: DotnetAspCSharpTypeName,
    name: String,
    modifiers: List<CSharp.Modifier> = emptyList(),
    attributes: List<CSharp.Attribute> = emptyList(),
    defaultValue: String? = null,
): CSharp.Parameter = csharpParameter(
    type = csharpType(type),
    name = name,
    modifiers = modifiers,
    attributes = attributes,
    defaultValue = defaultValue,
)

internal fun csharpParameter(
    type: String,
    name: String,
    modifiers: List<CSharp.Modifier> = emptyList(),
    attributes: List<CSharp.Attribute> = emptyList(),
    defaultValue: String? = null,
): CSharp.Parameter = csharpParameter(
    type = csharpType(type),
    name = name,
    modifiers = modifiers,
    attributes = attributes,
    defaultValue = defaultValue,
)

internal fun csharpAutoProperty(
    type: CSharp.TypeRef,
    name: String,
    modifiers: List<CSharp.Modifier>,
    attributes: List<CSharp.Attribute> = emptyList(),
    accessors: CSharp.PropertyAccessors = CSharp.PropertyAccessors.READ_WRITE,
    initializer: String? = null,
): CSharp.Property = CSharp.Property(
    type = type,
    name = name,
    modifiers = modifiers,
    attributes = attributes,
    accessors = accessors,
    initializer = initializer,
)

internal fun csharpType(name: DotnetAspCSharpTypeName): CSharp.TypeRef = csharpType(name.value)

internal fun csharpType(name: String): CSharp.TypeRef = CSharp.type(name)

internal fun csharpNullableType(name: DotnetAspCSharpTypeName): CSharp.TypeRef = csharpNullableType(name.value)

internal fun csharpNullableType(name: String): CSharp.TypeRef = CSharp.nullable(csharpType(name))

internal fun csharpGenericType(name: DotnetAspCSharpTypeName, vararg arguments: CSharp.TypeRef): CSharp.TypeRef =
    csharpGenericType(name.value, *arguments)

internal fun csharpGenericType(name: String, vararg arguments: CSharp.TypeRef): CSharp.TypeRef =
    CSharp.genericType(name, *arguments)

internal fun csharpArrayType(elementType: CSharp.TypeRef): CSharp.TypeRef = CSharp.array(elementType)

internal fun csharpTupleType(vararg elements: CSharp.TupleElement): CSharp.TypeRef = CSharp.tuple(*elements)

internal fun csharpTupleElement(type: CSharp.TypeRef, name: String? = null): CSharp.TupleElement =
    CSharp.tupleElement(type, name)

internal fun CSharpFileBuilder.using(namespace: DotnetAspCSharpNamespace) {
    using(namespace.value)
}

internal fun extensionParameter(type: DotnetAspCSharpTypeName, name: String): CSharp.Parameter =
    extensionParameter(type.value, name)

internal fun extensionParameter(type: String, name: String): CSharp.Parameter = csharpParameter(
    type = type,
    name = name,
    modifiers = listOf(CSharp.Modifier.THIS),
)
