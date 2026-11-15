package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
public interface DotnetAspResponsesScope {
    public fun ok(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_OK_STATUS, modelName, block)
    }

    public fun created(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_CREATED_STATUS, modelName, block)
    }

    public fun accepted(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_ACCEPTED_STATUS, modelName, block)
    }

    public fun noContent(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_NO_CONTENT_STATUS, modelName, block)
    }

    public fun badRequest(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_BAD_REQUEST_STATUS, modelName, block)
    }

    public fun unauthorized(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_UNAUTHORIZED_STATUS, modelName, block)
    }

    public fun forbidden(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_FORBIDDEN_STATUS, modelName, block)
    }

    public fun notFound(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_NOT_FOUND_STATUS, modelName, block)
    }

    public fun conflict(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_CONFLICT_STATUS, modelName, block)
    }

    public fun internalServerError(modelName: String, block: DotnetAspResponseScope.() -> Unit = {}) {
        status(HTTP_INTERNAL_SERVER_ERROR_STATUS, modelName, block)
    }

    public fun status(code: Int, modelName: String, block: DotnetAspResponseScope.() -> Unit = {})
}
