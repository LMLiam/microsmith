package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.InlineDotnetModelBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelScope

internal class DotnetAspResponseBuilder(
    private val modelName: String,
) : DotnetAspResponseScope {
    private var inlineModel: DotnetModel? = null
    private var headers: List<DotnetAspResponseHeader> = emptyList()

    override fun model(block: DotnetModelScope.() -> Unit) {
        require(inlineModel == null) {
            "ASP.NET response '$modelName' already declares an inline model."
        }
        inlineModel = InlineDotnetModelBuilder(modelName).apply(block).build()
    }

    override fun headers(block: DotnetAspResponseHeadersScope.() -> Unit) {
        require(headers.isEmpty()) {
            "ASP.NET response '$modelName' already declares headers metadata."
        }
        headers = DotnetAspResponseHeadersBuilder().apply(block).build()
    }

    fun build(statusCode: Int) = DotnetAspResponse(
        statusCode = statusCode,
        model =
        inlineModel?.let(DotnetAspModelReference::Inline)
            ?: DotnetAspModelReference.Shared(modelName),
        headers = headers,
    )
}
