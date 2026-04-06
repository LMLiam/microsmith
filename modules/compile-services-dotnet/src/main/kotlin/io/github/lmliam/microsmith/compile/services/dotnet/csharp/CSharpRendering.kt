package io.github.lmliam.microsmith.compile.services.dotnet.csharp

internal fun renderCSharp(file: CSharp.File): String = buildString {
    file.usings
        .sorted()
        .forEach { namespace ->
            appendLine("using $namespace;")
        }
    if (file.usings.isNotEmpty()) {
        appendLine()
    }
    appendLine("namespace ${file.namespace};")
    appendLine()
    append(file.types.joinToString("\n\n", transform = ::renderType))
}

private fun renderType(type: CSharp.Type): String = buildString {
    renderAttributes(type.attributes).forEach(::appendLine)
    append(type.modifiers.joinToString(" "))
    append(" ")
    append(type.kind.name.lowercase())
    append(" ")
    append(type.name)
    append(renderPrimaryConstructor(type.primaryConstructorParameters))
    if (type.baseTypes.isNotEmpty()) {
        append(" : ")
        append(type.baseTypes.joinToString(", ", transform = ::renderTypeRef))
    }
    if (type.members.isEmpty()) {
        append(";")
        return@buildString
    }
    appendLine()
    appendLine("{")
    append(indent(type.members.joinToString("\n\n", transform = ::renderMember)))
    appendLine()
    append("}")
}

private fun renderPrimaryConstructor(parameters: List<CSharp.Parameter>): String =
    parameters.takeIf(List<CSharp.Parameter>::isNotEmpty)?.let { items ->
        items.joinToString(
            prefix = "(",
            postfix = ")",
            transform = ::renderParameter,
        )
    }.orEmpty()

private fun renderMember(member: CSharp.Member): String {
    return when (member) {
        is CSharp.Method -> renderMethod(member)
        is CSharp.Property -> renderProperty(member)
    }
}

private fun renderProperty(property: CSharp.Property): String = buildString {
    renderAttributes(property.attributes).forEach(::appendLine)
    append(property.modifiers.joinToString(" "))
    append(" ")
    append(renderTypeRef(property.type))
    append(" ")
    append(property.name)
    append(" { ")
    append(property.getter)
    append(" ")
    append(property.setter)
    append(" }")
    property.initializer?.let { initializer ->
        append(" = ")
        append(initializer)
        append(";")
    }
}

private fun renderMethod(method: CSharp.Method): String = buildString {
    renderAttributes(method.attributes).forEach(::appendLine)
    append(method.modifiers.joinToString(" "))
    append(" ")
    append(renderTypeRef(method.returnType))
    append(" ")
    append(method.name)
    append(renderMethodParameters(method.parameters))
    val body = method.body
    if (body == null) {
        append(";")
        return@buildString
    }
    appendLine()
    appendLine("{")
    append(renderCodeBlock(body))
    appendLine()
    append("}")
}

private fun renderMethodParameters(parameters: List<CSharp.Parameter>): String {
    return when {
        parameters.isEmpty() -> "()"
        parameters.size == 1 -> "(${renderParameter(parameters.single())})"
        else -> parameters.joinToString(
            prefix = "(\n",
            postfix = "\n)",
            separator = ",\n",
            transform = { parameter -> indent(renderParameter(parameter), spaces = 4) },
        )
    }
}

private fun renderParameter(parameter: CSharp.Parameter): String = buildString {
    parameter.attributes.forEach { attribute ->
        append(renderAttribute(attribute))
        append(" ")
    }
    if (parameter.modifiers.isNotEmpty()) {
        append(parameter.modifiers.joinToString(" "))
        append(" ")
    }
    append(renderTypeRef(parameter.type))
    append(" ")
    append(parameter.name)
    parameter.defaultValue?.let { defaultValue ->
        append(" = ")
        append(defaultValue)
    }
}

private fun renderAttributes(attributes: List<CSharp.Attribute>): List<String> = attributes.map(::renderAttribute)
