package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution
import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModelLocality
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspBindingResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue

internal class DotnetAspModelResolver {
    fun resolve(
        context: DotnetAspOperationContext,
        models: Map<String, DotnetModel>,
        reference: DotnetAspModelReference,
        source: DotnetAspBindingResolutionIssue.ModelReferenceSource,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspModel> = when (reference) {
        is DotnetAspModelReference.Shared -> resolveShared(context, models, reference.target, source)
        is DotnetAspModelReference.Inline -> resolveInline(context, models, reference.model)
    }

    private fun resolveShared(
        context: DotnetAspOperationContext,
        models: Map<String, DotnetModel>,
        target: String,
        source: DotnetAspBindingResolutionIssue.ModelReferenceSource,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspModel> {
        val model = models[target]

        return if (model != null) {
            Either.Right(ResolvedDotnetAspModel(locality = ResolvedDotnetAspModelLocality.SHARED, model))
        } else {
            Either.Left(
                nonEmptyListOf(
                    DotnetAspBindingResolutionIssue.UnknownSharedModelReference(
                        context.serviceName,
                        context.operationName,
                        source,
                        target,
                    ),
                ),
            )
        }
    }

    private fun resolveInline(
        context: DotnetAspOperationContext,
        models: Map<String, DotnetModel>,
        model: DotnetModel,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspModel> {
        val issues = model.fields.mapNotNull { field ->
            val reference = field.type as? DotnetFieldType.Reference ?: return@mapNotNull null
            if (reference.target in models) return@mapNotNull null

            DotnetAspBindingResolutionIssue.UnknownSharedModelReference(
                context.serviceName,
                context.operationName,
                DotnetAspBindingResolutionIssue.ModelReferenceSource.InlineModelField(model.name, field.name),
                reference.target,
            )
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            Either.Left(accumulatedIssues)
        } else {
            Either.Right(ResolvedDotnetAspModel(locality = ResolvedDotnetAspModelLocality.INLINE, model))
        }
    }
}
