package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspResponsesScope {
    fun ok(modelName: String)

    fun ok(modelName: String, block: DotnetAspResponseScope.() -> Unit)

    fun created(modelName: String)

    fun created(modelName: String, block: DotnetAspResponseScope.() -> Unit)

    fun badRequest(modelName: String)

    fun badRequest(modelName: String, block: DotnetAspResponseScope.() -> Unit)

    fun notFound(modelName: String)

    fun notFound(modelName: String, block: DotnetAspResponseScope.() -> Unit)

    fun status(code: Int, modelName: String)

    fun status(code: Int, modelName: String, block: DotnetAspResponseScope.() -> Unit)
}
