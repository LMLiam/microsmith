package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelScope

private const val HTTP_OK_STATUS = 200
private const val HTTP_CREATED_STATUS = 201
private const val HTTP_BAD_REQUEST_STATUS = 400
private const val HTTP_NOT_FOUND_STATUS = 404

internal class DotnetAspResponsesBuilder : DotnetAspResponsesScope {
    private val responsesByStatus = linkedMapOf<Int, DotnetAspResponse>()

    override fun ok(modelName: String) = addResponse(HTTP_OK_STATUS, modelName, null)
    override fun ok(modelName: String, block: DotnetAspResponseScope.() -> Unit) =
        addResponse(HTTP_OK_STATUS, modelName, block)

    override fun created(modelName: String) = addResponse(HTTP_CREATED_STATUS, modelName, null)
    override fun created(modelName: String, block: DotnetAspResponseScope.() -> Unit) =
        addResponse(HTTP_CREATED_STATUS, modelName, block)

    override fun badRequest(modelName: String) = addResponse(HTTP_BAD_REQUEST_STATUS, modelName, null)
    override fun badRequest(modelName: String, block: DotnetAspResponseScope.() -> Unit) =
        addResponse(HTTP_BAD_REQUEST_STATUS, modelName, block)

    override fun notFound(modelName: String) = addResponse(HTTP_NOT_FOUND_STATUS, modelName, null)
    override fun notFound(modelName: String, block: DotnetAspResponseScope.() -> Unit) =
        addResponse(HTTP_NOT_FOUND_STATUS, modelName, block)

    override fun status(code: Int, modelName: String) = addResponse(code, modelName, null)
    override fun status(code: Int, modelName: String, block: DotnetAspResponseScope.() -> Unit) =
        addResponse(code, modelName, block)

    fun build(): List<DotnetAspResponse> = responsesByStatus.values.toList()

    private fun addResponse(statusCode: Int, modelName: String, block: (DotnetAspResponseScope.() -> Unit)?) {
        require(statusCode !in responsesByStatus) {
            "ASP.NET responses already declare status $statusCode."
        }

        val scope = DotnetAspResponseBuilder(modelName)
        block?.let(scope::apply)
        responsesByStatus[statusCode] = scope.build(statusCode)
    }
}

private class DotnetAspResponseBuilder(
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

private class DotnetAspResponseHeadersBuilder : DotnetAspResponseHeadersScope {
    private val headers = mutableListOf<DotnetAspResponseHeader>()

    override fun header(name: String): DotnetAspResponseHeader {
        val header = DotnetAspResponseHeader(name.trim())
        headers += header
        return header
    }

    fun build() = headers.toList()
}
