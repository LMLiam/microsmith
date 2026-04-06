package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

internal class DotnetAspRequestBindingBuilder(
    private val name: String,
) : AbstractDotnetAspRequestBindingBuilder() {
    private val fieldsByName = linkedMapOf<String, DotnetAspRequestField>()

    fun build() = DotnetAspRequestBinding(name = name, fields = fieldsByName.values.toList())

    override fun addField(
        name: String,
        type: DotnetFieldType,
        block: DotnetAspRequestFieldScope.() -> Unit,
    ): DotnetAspRequestField {
        val options = DotnetAspRequestFieldOptions().apply(block)
        return registerField(
            DotnetAspRequestField(
                name = name,
                type = type,
                optional = options.optional,
                defaultValue = options.defaultValue,
            ),
        )
    }

    override fun addReference(name: String, target: String): DotnetAspRequestField = registerField(
        DotnetAspRequestField(name = name, type = DotnetFieldType.Reference(target)),
    )

    private fun registerField(field: DotnetAspRequestField): DotnetAspRequestField {
        require(field.name !in fieldsByName) {
            "Duplicate ASP.NET request field '${field.name}' in binding '$name'."
        }
        fieldsByName[field.name] = field
        return field
    }
}

private class DotnetAspRequestFieldOptions : DotnetAspRequestFieldScope {
    var optional = false
        private set
    var defaultValue: Any? = null
        private set

    override fun optional() {
        require(!optional) { "optional() already set for ASP.NET request field." }
        optional = true
    }

    override fun default(value: Any) {
        require(defaultValue == null) { "default(...) already set for ASP.NET request field." }
        defaultValue = value
    }
}
