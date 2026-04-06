package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun renderTypeRef(type: CSharp.TypeRef): String {
    return when (type) {
        is CSharp.ArrayType -> "${renderTypeRef(type.elementType)}[]"
        is CSharp.GenericType -> type.arguments.joinToString(
            prefix = "${type.name}<",
            postfix = ">",
            transform = ::renderTypeRef,
        )
        is CSharp.NamedType -> type.name
        is CSharp.NullableType -> "${renderTypeRef(type.underlying)}?"
        is CSharp.TupleType -> type.elements.joinToString(
            prefix = "(",
            postfix = ")",
            transform = ::renderTupleElement,
        )
    }
}

private fun renderTupleElement(element: CSharp.TupleElement): String {
    return element.name?.let { name -> "${renderTypeRef(element.type)} $name" }
        ?: renderTypeRef(element.type)
}
