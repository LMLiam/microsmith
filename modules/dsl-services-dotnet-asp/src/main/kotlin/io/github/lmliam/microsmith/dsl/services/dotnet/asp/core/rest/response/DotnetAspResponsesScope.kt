package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspResponsesScope {
    fun ok(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_OK_STATUS, modelName, block)
    }

    fun created(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_CREATED_STATUS, modelName, block)
    }

    fun accepted(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_ACCEPTED_STATUS, modelName, block)
    }

    fun noContent(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_NO_CONTENT_STATUS, modelName, block)
    }

    fun badRequest(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_BAD_REQUEST_STATUS, modelName, block)
    }

    fun unauthorized(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_UNAUTHORIZED_STATUS, modelName, block)
    }

    fun forbidden(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_FORBIDDEN_STATUS, modelName, block)
    }

    fun notFound(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_NOT_FOUND_STATUS, modelName, block)
    }

    fun conflict(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_CONFLICT_STATUS, modelName, block)
    }

    fun internalServerError(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_INTERNAL_SERVER_ERROR_STATUS, modelName, block)
    }

    fun status(code: Int, modelName: String, block: DotnetAspResponseScope.() -> Unit = {})
}
