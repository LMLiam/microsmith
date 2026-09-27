package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution
import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeaderField
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspHeadersBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestBinding
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRequestField
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspBindingResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue

internal class DotnetAspBindingResolver {
    fun resolvePathBinding(
        context: DotnetAspOperationContext,
        placeholders: List<String>,
        binding: DotnetAspRequestBinding,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspRequestBinding> {
        val issues = buildList<DotnetAspResolutionIssue> {
            addAll(requestBindingIssues(context, binding))

            binding.fields
                .filter { it.optional }
                .forEach { field ->
                    add(
                        DotnetAspBindingResolutionIssue.OptionalPathBindingField(
                            context.serviceName,
                            context.operationName,
                            binding.name,
                            field.name,
                        ),
                    )
                }

            binding.fields
                .filter { it.defaultValue != null }
                .forEach { field ->
                    add(
                        DotnetAspBindingResolutionIssue.DefaultedPathBindingField(
                            context.serviceName,
                            context.operationName,
                            binding.name,
                            field.name,
                        ),
                    )
                }

            val fields = binding.fields.map { it.name }

            if (fields.toSet() != placeholders.toSet()) {
                add(
                    DotnetAspBindingResolutionIssue.PathBindingFieldMismatch(
                        context.serviceName,
                        context.operationName,
                        binding.name,
                        placeholders,
                        fields,
                    ),
                )
            }
        }

        return resolveOrIssues(binding, issues)
    }

    fun resolveRequestBinding(
        context: DotnetAspOperationContext,
        binding: DotnetAspRequestBinding,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspRequestBinding> =
        resolveOrIssues(binding, issues = requestBindingIssues(context, binding))

    fun resolveHeadersBinding(binding: DotnetAspHeadersBinding): ResolvedDotnetAspHeadersBinding =
        ResolvedDotnetAspHeadersBinding(
            binding.name,
            headers = binding.headers.map { field ->
                ResolvedDotnetAspHeaderField(field.name, field.headerName)
            },
        )

    private fun requestBindingIssues(
        context: DotnetAspOperationContext,
        binding: DotnetAspRequestBinding,
    ): List<DotnetAspBindingResolutionIssue> = binding.fields.mapNotNull { field ->
        val reference = field.type as? DotnetFieldType.Reference ?: return@mapNotNull null

        DotnetAspBindingResolutionIssue.RequestBindingReferenceField(
            context.serviceName,
            context.operationName,
            binding.name,
            field.name,
            reference.target,
        )
    }

    private fun resolveOrIssues(
        binding: DotnetAspRequestBinding,
        issues: List<DotnetAspResolutionIssue>,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspRequestBinding> {
        val accumulatedIssues = issues.toNonEmptyListOrNull()

        if (accumulatedIssues != null) {
            return Either.Left(accumulatedIssues)
        }

        return Either.Right(
            ResolvedDotnetAspRequestBinding(
                binding.name,
                fields = binding.fields.map { field ->
                    ResolvedDotnetAspRequestField(
                        field.name,
                        field.type,
                        optional = field.optional || field.defaultValue != null,
                        field.defaultValue,
                    )
                },
            ),
        )
    }
}
