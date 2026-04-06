package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspRequestBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

internal class DotnetAspBindingResolver {
    fun resolvePathBinding(
        serviceName: String,
        endpoint: DotnetAspEndpoint,
        placeholders: List<String>,
        binding: DotnetAspRequestBinding,
        models: Map<String, DotnetModel>,
    ): ResolvedDotnetAspRequestBinding {
        val resolved = resolveRequestBinding(serviceName, endpoint.operationName, binding, models)

        resolved.fields.forEach { field ->
            require(!field.optional && field.defaultValue == null) {
                "ASP.NET path binding '${binding.name}' in operation '${endpoint.operationName}' " +
                    "cannot declare optional/default fields."
            }
        }

        val bindingFields = resolved.fields.mapTo(mutableSetOf(), ResolvedDotnetAspRequestField::name)
        val placeholderSet = placeholders.toSet()
        require(bindingFields == placeholderSet) {
            "ASP.NET path binding '${binding.name}' in operation '${endpoint.operationName}' " +
                "must match route placeholders ${placeholders.joinToString(", ")}, " +
                "but declared ${resolved.fields.map(ResolvedDotnetAspRequestField::name).joinToString(", ")}."
        }

        return resolved
    }

    fun resolveRequestBinding(
        serviceName: String,
        operationName: String,
        binding: DotnetAspRequestBinding,
        models: Map<String, DotnetModel>,
    ): ResolvedDotnetAspRequestBinding {
        binding.fields
            .mapNotNull { it.type as? DotnetFieldType.Reference }
            .forEach { reference ->
                require(reference.target in models) {
                    "ASP.NET request binding '${binding.name}' in operation '$operationName' " +
                        "for service '$serviceName' references unknown shared model '${reference.target}'."
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
        serviceName: String,
        operationName: String,
        models: Map<String, DotnetModel>,
        reference: DotnetAspModelReference,
    ): ResolvedDotnetAspModel = when (reference) {
        is DotnetAspModelReference.Inline -> ResolvedDotnetAspModel(
            locality = ResolvedDotnetAspModelLocality.INLINE,
            model = validateInlineReferences(serviceName, operationName, reference.model, models),
        )

        is DotnetAspModelReference.Shared -> ResolvedDotnetAspModel(
            locality = ResolvedDotnetAspModelLocality.SHARED,
            model =
            requireNotNull(models[reference.target]) {
                "ASP.NET operation '$operationName' in service '$serviceName' " +
                    "references unknown model '${reference.target}'."
            },
        )
    }

    private fun validateInlineReferences(
        serviceName: String,
        operationName: String,
        model: DotnetModel,
        models: Map<String, DotnetModel>,
    ): DotnetModel {
        model.fields
            .mapNotNull { it.type as? DotnetFieldType.Reference }
            .forEach { reference ->
                require(reference.target in models) {
                    "ASP.NET inline model '${model.name}' in operation '$operationName' " +
                        "for service '$serviceName' references unknown shared model '${reference.target}'."
                }
            }

        return model
    }
}
