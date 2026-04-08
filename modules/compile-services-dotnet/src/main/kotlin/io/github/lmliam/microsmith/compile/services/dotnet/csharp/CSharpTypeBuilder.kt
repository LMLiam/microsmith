package io.github.lmliam.microsmith.compile.services.dotnet.csharp

@CSharp.Dsl
class CSharpTypeBuilder internal constructor(
    private val kind: CSharp.TypeKind,
    private val name: String,
    private val modifiers: List<CSharp.Modifier>,
    private val baseTypes: List<CSharp.TypeRef> = emptyList(),
    private val attributes: List<CSharp.Attribute> = emptyList(),
    private val primaryConstructorParameters: List<CSharp.Parameter> = emptyList(),
) {
    private val members = mutableListOf<CSharp.Member>()

    fun addMember(member: CSharp.Member) {
        members += member
    }

    fun property(
        type: CSharp.TypeRef,
        name: String,
        modifiers: List<CSharp.Modifier>,
        attributes: List<CSharp.Attribute> = emptyList(),
        accessors: CSharp.PropertyAccessors = CSharp.PropertyAccessors.READ_WRITE,
        initializer: String? = null,
    ) {
        members += CSharp.Property(
            type = type,
            name = name,
            modifiers = modifiers,
            attributes = attributes,
            accessors = accessors,
            initializer = initializer,
        )
    }

    fun method(
        name: String,
        returnType: CSharp.TypeRef,
        modifiers: List<CSharp.Modifier>,
        attributes: List<CSharp.Attribute> = emptyList(),
        parameters: List<CSharp.Parameter> = emptyList(),
        body: CSharp.CodeBlock? = null,
    ) {
        members += CSharp.Method(
            name = name,
            modifiers = modifiers,
            returnType = returnType,
            attributes = attributes,
            parameters = parameters,
            body = body,
        )
    }

    internal fun build(): CSharp.Type = CSharp.Type(
        kind = kind,
        name = name,
        modifiers = modifiers,
        baseTypes = baseTypes,
        attributes = attributes,
        primaryConstructorParameters = primaryConstructorParameters,
        members = members,
    )

    fun property(
        type: String,
        name: String,
        modifiers: List<CSharp.Modifier>,
        attributes: List<CSharp.Attribute> = emptyList(),
        accessors: CSharp.PropertyAccessors = CSharp.PropertyAccessors.READ_WRITE,
        initializer: String? = null,
    ) {
        property(
            type = CSharp.type(type),
            name = name,
            modifiers = modifiers,
            attributes = attributes,
            accessors = accessors,
            initializer = initializer,
        )
    }

    fun method(
        name: String,
        returnType: String,
        modifiers: List<CSharp.Modifier>,
        attributes: List<CSharp.Attribute> = emptyList(),
        parameters: List<CSharp.Parameter> = emptyList(),
        body: CSharp.CodeBlock? = null,
    ) {
        method(
            name = name,
            returnType = CSharp.type(returnType),
            modifiers = modifiers,
            attributes = attributes,
            parameters = parameters,
            body = body,
        )
    }
}
