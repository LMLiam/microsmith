package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response.DotnetAspResponse
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponseHeader
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspBindingResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspGenerationResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.HTTP_NO_CONTENT_STATUS_CODE
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.RESULT_BODY_PROPERTY_NAME
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspHeaderPropertyName

internal class DotnetAspResponseResolver {
    private val modelResolver = DotnetAspModelResolver()

    fun resolve(
        context: DotnetAspOperationContext,
        endpoint: DotnetAspEndpoint,
        models: Map<String, DotnetModel>,
    ): EitherNel<DotnetAspResolutionIssue, List<ResolvedDotnetAspResponse>> {
        val issues = mutableListOf<DotnetAspResolutionIssue>()
        val resolved = mutableListOf<ResolvedDotnetAspResponse>()

        endpoint.responses.forEach { response ->
            modelResolver
                .resolve(
                    context,
                    models,
                    response.model,
                    DotnetAspBindingResolutionIssue.ModelReferenceSource.Response(response.statusCode),
                )
                .fold(
                    ifLeft = issues::addAll,
                    ifRight = { model ->
                        issues += generationIssues(context, response, model)
                        resolved +=
                            ResolvedDotnetAspResponse(
                                response.statusCode,
                                model,
                                headers = response.headers.map { ResolvedDotnetAspResponseHeader(it.name) },
                            )
                    },
                )
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            Either.Left(accumulatedIssues)
        } else {
            Either.Right(resolved)
        }
    }

    private fun generationIssues(
        context: DotnetAspOperationContext,
        response: DotnetAspResponse,
        model: ResolvedDotnetAspModel,
    ): List<DotnetAspGenerationResolutionIssue> = buildList {
        if (response.statusCode == HTTP_NO_CONTENT_STATUS_CODE && model.model.fields.isNotEmpty()) {
            add(
                DotnetAspGenerationResolutionIssue.NoContentResponseWithBody(
                    context.serviceName,
                    context.operationName,
                    model.model.name,
                )
            )
        }
        addAll(headerIssues(context, response))
    }

    private fun headerIssues(
        context: DotnetAspOperationContext,
        response: DotnetAspResponse,
    ): List<DotnetAspGenerationResolutionIssue> {
        val propertyNames = response.headers.associateWith { header -> dotnetAspHeaderPropertyName(header.name) }

        return buildList {
            propertyNames.forEach { (header, propertyName) ->
                when (propertyName) {
                    null ->
                        add(
                            DotnetAspGenerationResolutionIssue.ResponseHeaderWithoutPropertyName(
                                context.serviceName,
                                context.operationName,
                                response.statusCode,
                                header.name,
                            )
                        )

                    RESULT_BODY_PROPERTY_NAME ->
                        add(
                            DotnetAspGenerationResolutionIssue.ResponseHeaderCollidesWithBody(
                                context.serviceName,
                                context.operationName,
                                response.statusCode,
                                header.name,
                            )
                        )

                    else -> Unit
                }
            }

            val collidingPropertyNames =
                propertyNames.values
                    .filterNotNull()
                    .filter { it != RESULT_BODY_PROPERTY_NAME }
                    .groupingBy { it }
                    .eachCount()
                    .filterValues { it > 1 }
                    .keys
                    .sorted()
            if (collidingPropertyNames.isNotEmpty()) {
                add(
                    DotnetAspGenerationResolutionIssue.ResponseHeaderPropertyCollision(
                        context.serviceName,
                        context.operationName,
                        response.statusCode,
                        collidingPropertyNames,
                    )
                )
            }
        }
    }
}
