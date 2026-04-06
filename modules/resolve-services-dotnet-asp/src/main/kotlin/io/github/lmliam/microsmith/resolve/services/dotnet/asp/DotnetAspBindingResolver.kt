package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspRequestBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

internal class DotnetAspBindingResolver {
    fun resolvePathBinding(
        context: DotnetAspOperationContext,
        placeholders: List<String>,
        binding: DotnetAspRequestBinding,
        models: Map<String, DotnetModel>,
    ): ResolvedDotnetAspRequestBinding {
        val resolved = resolveRequestBinding(context, binding, models)

        resolved.fields.forEach { field ->
            require(!field.optional && field.defaultValue == null) {
                "ASP.NET path binding '${binding.name}' in operation '${context.operationName}' " +
                    "cannot declare optional/default fields."
            }
        }

        val bindingFields = resolved.fields.mapTo(mutableSetOf(), ResolvedDotnetAspRequestField::name)
        val placeholderSet = placeholders.toSet()
        require(bindingFields == placeholderSet) {
            "ASP.NET path binding '${binding.name}' in operation '${context.operationName}' " +
                "must match route placeholders ${placeholders.joinToString(", ")}, " +
                "but declared ${resolved.fields.joinToString(", ", transform = ResolvedDotnetAspRequestField::name)}."
        }

        return resolved
    }

    fun resolveRequestBinding(
        context: DotnetAspOperationContext,
        binding: DotnetAspRequestBinding,
        models: Map<String, DotnetModel>,
    ): ResolvedDotnetAspRequestBinding {
        binding.fields
            .mapNotNull { it.type as? DotnetFieldType.Reference }
            .forEach { reference ->
                require(reference.target in models) {
                    "ASP.NET request binding '${binding.name}' in operation '${context.operationName}' " +
                        "for service '${context.serviceName}' references unknown shared model '${reference.target}'."
                }
            }

        return ResolvedDotnetAspRequestBinding(
            name = binding.name,
            fields = binding.fields.map { field ->
                ResolvedDotnetAspRequestField(
                    name = field.name,
                    type = field.type,
                    optional = field.optional || field.defaultValue != null,
                    defaultValue = field.defaultValue,
                )
            },
        )
    }

    fun resolveHeadersBinding(binding: DotnetAspHeadersBinding) = ResolvedDotnetAspHeadersBinding(
        name = binding.name,
        headers = binding.headers.map { field ->
            ResolvedDotnetAspHeaderField(name = field.name, headerName = field.headerName)
        },
    )

    fun resolveModelReference(
        context: DotnetAspOperationContext,
        models: Map<String, DotnetModel>,
        reference: DotnetAspModelReference,
    ): ResolvedDotnetAspModel = when (reference) {
        is DotnetAspModelReference.Inline -> ResolvedDotnetAspModel(
            locality = ResolvedDotnetAspModelLocality.INLINE,
            model = validateInlineReferences(context, reference.model, models),
        )

        is DotnetAspModelReference.Shared -> ResolvedDotnetAspModel(
            locality = ResolvedDotnetAspModelLocality.SHARED,
            model =
            requireNotNull(models[reference.target]) {
                "ASP.NET operation '${context.operationName}' in service '${context.serviceName}' " +
                    "references unknown model '${reference.target}'."
            },
        )
    }

    private fun validateInlineReferences(
        context: DotnetAspOperationContext,
        model: DotnetModel,
        models: Map<String, DotnetModel>,
    ): DotnetModel {
        model.fields
            .mapNotNull { it.type as? DotnetFieldType.Reference }
            .forEach { reference ->
                require(reference.target in models) {
                    "ASP.NET inline model '${model.name}' in operation '${context.operationName}' " +
                        "for service '${context.serviceName}' references unknown shared model '${reference.target}'."
                }
            }

        return model
    }
}
