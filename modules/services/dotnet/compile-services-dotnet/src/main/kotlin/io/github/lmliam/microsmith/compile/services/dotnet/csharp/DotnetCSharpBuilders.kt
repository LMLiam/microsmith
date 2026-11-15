package io.github.lmliam.microsmith.compile.services.dotnet.csharp

public fun csharpParameter(
    type: CSharp.TypeRef,
    name: String,
    modifiers: List<CSharp.Modifier> = emptyList(),
    attributes: List<CSharp.Attribute> = emptyList(),
    defaultValue: String? = null,
): CSharp.Parameter =
    CSharp.Parameter(
        type = type,
        name = name,
        modifiers = modifiers,
        attributes = attributes,
        defaultValue = defaultValue,
    )

public fun csharpParameter(
    type: DotnetCSharpTypeName,
    name: String,
    modifiers: List<CSharp.Modifier> = emptyList(),
    attributes: List<CSharp.Attribute> = emptyList(),
    defaultValue: String? = null,
): CSharp.Parameter =
    csharpParameter(
        type = csharpType(type),
        name = name,
        modifiers = modifiers,
        attributes = attributes,
        defaultValue = defaultValue,
    )

public fun csharpParameter(
    type: String,
    name: String,
    modifiers: List<CSharp.Modifier> = emptyList(),
    attributes: List<CSharp.Attribute> = emptyList(),
    defaultValue: String? = null,
): CSharp.Parameter =
    csharpParameter(
        type = csharpType(type),
        name = name,
        modifiers = modifiers,
        attributes = attributes,
        defaultValue = defaultValue,
    )

public fun csharpAutoProperty(
    type: CSharp.TypeRef,
    name: String,
    modifiers: List<CSharp.Modifier>,
    attributes: List<CSharp.Attribute> = emptyList(),
    accessors: CSharp.PropertyAccessors = CSharp.PropertyAccessors.READ_WRITE,
    initializer: String? = null,
): CSharp.Property =
    CSharp.Property(
        type = type,
        name = name,
        modifiers = modifiers,
        attributes = attributes,
        accessors = accessors,
        initializer = initializer,
    )

public fun csharpType(name: DotnetCSharpTypeName): CSharp.TypeRef = csharpType(name.value)

public fun csharpType(name: String): CSharp.TypeRef = CSharp.NamedType(name)

public fun csharpNullableType(name: DotnetCSharpTypeName): CSharp.TypeRef = csharpNullableType(name.value)

public fun csharpNullableType(name: String): CSharp.TypeRef = CSharp.NullableType(csharpType(name))

public fun csharpGenericType(name: DotnetCSharpTypeName, vararg arguments: CSharp.TypeRef): CSharp.TypeRef =
    csharpGenericType(name.value, *arguments)

public fun csharpGenericType(name: String, vararg arguments: CSharp.TypeRef): CSharp.TypeRef =
    CSharp.GenericType(name, arguments.toList())

public fun csharpArrayType(elementType: CSharp.TypeRef): CSharp.TypeRef = CSharp.ArrayType(elementType)

public fun csharpTupleType(vararg elements: CSharp.TupleElement): CSharp.TypeRef = CSharp.TupleType(elements.toList())

public fun csharpTupleElement(type: CSharp.TypeRef, name: String? = null): CSharp.TupleElement =
    CSharp.TupleElement(type, name)

public fun CSharpFileBuilder.using(namespace: DotnetCSharpNamespace) {
    using(namespace.value)
}

public fun extensionParameter(type: DotnetCSharpTypeName, name: String): CSharp.Parameter =
    extensionParameter(type.value, name)

public fun extensionParameter(type: String, name: String): CSharp.Parameter =
    csharpParameter(
        type = type,
        name = name,
        modifiers = listOf(CSharp.Modifier.THIS),
    )
