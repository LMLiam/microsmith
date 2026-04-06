package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

internal const val HTTP_OK_STATUS = 200
internal const val HTTP_CREATED_STATUS = 201
internal const val HTTP_ACCEPTED_STATUS = 202
internal const val HTTP_NO_CONTENT_STATUS = 204
internal const val HTTP_BAD_REQUEST_STATUS = 400
internal const val HTTP_UNAUTHORIZED_STATUS = 401
internal const val HTTP_FORBIDDEN_STATUS = 403
internal const val HTTP_NOT_FOUND_STATUS = 404
internal const val HTTP_CONFLICT_STATUS = 409
internal const val HTTP_INTERNAL_SERVER_ERROR_STATUS = 500

internal class DotnetAspResponsesBuilder : DotnetAspResponsesScope {
    private val responsesByStatus = linkedMapOf<Int, DotnetAspResponse>()

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
