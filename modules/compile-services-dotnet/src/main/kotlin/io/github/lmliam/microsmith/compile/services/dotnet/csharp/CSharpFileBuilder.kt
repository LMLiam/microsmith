package io.github.lmliam.microsmith.compile.services.dotnet.csharp

@CSharp.Dsl
class CSharpFileBuilder internal constructor(
    private val namespace: String,
) {
    private val usings = linkedSetOf<String>()
    private val types = mutableListOf<CSharp.Type>()

    fun using(namespace: String) {
        usings += namespace
    }

    fun addType(type: CSharp.Type) {
        types += type
    }

    fun classType(
        name: String,
        modifiers: List<CSharp.Modifier>,
        baseTypes: List<CSharp.TypeRef> = emptyList(),
        attributes: List<CSharp.Attribute> = emptyList(),
        build: CSharpTypeBuilder.() -> Unit = {},
    ) {
        types += CSharpTypeBuilder(
            kind = CSharp.TypeKind.CLASS,
            name = name,
            modifiers = modifiers,
            baseTypes = baseTypes,
            attributes = attributes,
        ).apply(build).build()
    }

    fun recordType(
        name: String,
        modifiers: List<CSharp.Modifier>,
        primaryConstructorParameters: List<CSharp.Parameter> = emptyList(),
        baseTypes: List<CSharp.TypeRef> = emptyList(),
        attributes: List<CSharp.Attribute> = emptyList(),
        build: CSharpTypeBuilder.() -> Unit = {},
    ) {
        types += CSharpTypeBuilder(
            kind = CSharp.TypeKind.RECORD,
            name = name,
            modifiers = modifiers,
            baseTypes = baseTypes,
            attributes = attributes,
            primaryConstructorParameters = primaryConstructorParameters,
        ).apply(build).build()
    }

    internal fun build(): CSharp.File = CSharp.File(
        namespace = namespace,
        usings = usings,
        types = types,
    )
}
