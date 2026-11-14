package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestField
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
        val issues =
            buildList<DotnetAspResolutionIssue> {
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

        return resolveOrIssues(context, binding, issues)
    }

    fun resolveRequestBinding(
        context: DotnetAspOperationContext,
        binding: DotnetAspRequestBinding,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspRequestBinding> =
        resolveOrIssues(context, binding, issues = requestBindingIssues(context, binding))

    fun resolveHeadersBinding(binding: DotnetAspHeadersBinding): ResolvedDotnetAspHeadersBinding =
        ResolvedDotnetAspHeadersBinding(
            binding.name,
            headers = binding.headers.map { field -> ResolvedDotnetAspHeaderField(field.name, field.headerName) },
        )

    private fun requestBindingIssues(
        context: DotnetAspOperationContext,
        binding: DotnetAspRequestBinding,
    ): List<DotnetAspBindingResolutionIssue> =
        binding.fields.mapNotNull { field ->
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
        context: DotnetAspOperationContext,
        binding: DotnetAspRequestBinding,
        issues: List<DotnetAspResolutionIssue>,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspRequestBinding> {
        val defaults = binding.fields.map { field -> resolveDefault(context, binding, field) }
        val accumulatedIssues = (issues + defaults.mapNotNull { it.leftOrNull() }).toNonEmptyListOrNull()

        if (accumulatedIssues != null) {
            return Either.Left(accumulatedIssues)
        }

        return Either.Right(
            ResolvedDotnetAspRequestBinding(
                binding.name,
                fields =
                    binding.fields.zip(defaults) { field, default ->
                        ResolvedDotnetAspRequestField(
                            field.name,
                            field.type,
                            optional = field.optional || field.defaultValue != null,
                            default.getOrNull(),
                        )
                    },
            ),
        )
    }

    private fun resolveDefault(
        context: DotnetAspOperationContext,
        binding: DotnetAspRequestBinding,
        field: DotnetAspRequestField,
    ): Either<DotnetAspBindingResolutionIssue, DotnetAspDefaultValue?> {
        val defaultValue = field.defaultValue ?: return Either.Right(null)
        val integerType = field.type.integerType() ?: return Either.Right(defaultValue)
        val number = (defaultValue as DotnetAspDefaultValue.NumericValue).value

        return number.toIntegerDefault(integerType).map(DotnetAspDefaultValue::NumericValue).mapLeft { problem ->
            when (problem) {
                IntegerDefaultProblem.NOT_INTEGRAL ->
                    DotnetAspBindingResolutionIssue.IntegerDefaultNotIntegral(
                        context.serviceName,
                        context.operationName,
                        binding.name,
                        field.name,
                        integerType.csharpName,
                        number.toString(),
                    )

                IntegerDefaultProblem.OUT_OF_RANGE ->
                    DotnetAspBindingResolutionIssue.IntegerDefaultOutOfRange(
                        context.serviceName,
                        context.operationName,
                        binding.name,
                        field.name,
                        integerType.csharpName,
                        number.toString(),
                    )
            }
        }
    }
}
