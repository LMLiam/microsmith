package io.github.lmliam.microsmith.compile.services.dotnet.csharp

@CSharp.Dsl
class CSharpTypeBuilder internal constructor(
    private val kind: CSharp.TypeKind,
    private val name: String,
    private val modifiers: List<String>,
    private val baseTypes: List<CSharp.TypeRef>,
    private val attributes: List<CSharp.Attribute>,
    private val primaryConstructorParameters: List<CSharp.Parameter>,
) {
    private val members = mutableListOf<CSharp.Member>()

    fun addMember(member: CSharp.Member) {
        members += member
    }

    fun property(
        type: CSharp.TypeRef,
        name: String,
        modifiers: List<String>,
        attributes: List<CSharp.Attribute> = emptyList(),
        getter: String = "get;",
        setter: String = "set;",
        initializer: String? = null,
    ) {
        members += CSharp.Property(
            type = type,
            name = name,
            modifiers = modifiers,
            attributes = attributes,
            getter = getter,
            setter = setter,
            initializer = initializer,
        )
    }

    fun method(
        name: String,
        returnType: CSharp.TypeRef,
        modifiers: List<String>,
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
        modifiers: List<String>,
        attributes: List<CSharp.Attribute> = emptyList(),
        getter: String = "get;",
        setter: String = "set;",
        initializer: String? = null,
    ) {
        property(
            type = CSharp.type(type),
            name = name,
            modifiers = modifiers,
            attributes = attributes,
            getter = getter,
            setter = setter,
            initializer = initializer,
        )
    }

    fun method(
        name: String,
        returnType: String,
        modifiers: List<String>,
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
