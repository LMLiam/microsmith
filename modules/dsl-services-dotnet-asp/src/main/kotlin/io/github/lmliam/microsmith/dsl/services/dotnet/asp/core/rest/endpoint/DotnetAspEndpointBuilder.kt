package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.DotnetAspModelReference
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.model.InlineDotnetModelBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspHeadersBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspHeadersBindingBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspHeadersBindingScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspRequestBinding
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspRequestBindingBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspRequestBindingScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response.DotnetAspResponse
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response.DotnetAspResponsesBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response.DotnetAspResponsesScope
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModelScope

internal class DotnetAspEndpointBuilder(
    private val method: DotnetAspHttpMethod,
    private val path: String,
    private val operationName: String,
) : DotnetAspEndpointScope {
    private var pathBinding: DotnetAspRequestBinding? = null
    private var queryBinding: DotnetAspRequestBinding? = null
    private var headersBinding: DotnetAspHeadersBinding? = null
    private var bodyBinding: DotnetAspModelReference? = null
    private var responses: List<DotnetAspResponse> = emptyList()

    override fun path(name: String, block: DotnetAspRequestBindingScope.() -> Unit) {
        require(pathBinding == null) {
            "ASP.NET endpoint '$operationName' already declares a path binding."
        }
        pathBinding = DotnetAspRequestBindingBuilder(name).apply(block).build()
    }

    override fun query(name: String, block: DotnetAspRequestBindingScope.() -> Unit) {
        require(queryBinding == null) {
            "ASP.NET endpoint '$operationName' already declares a query binding."
        }
        queryBinding = DotnetAspRequestBindingBuilder(name).apply(block).build()
    }

    override fun headers(name: String, block: DotnetAspHeadersBindingScope.() -> Unit) {
        require(headersBinding == null) {
            "ASP.NET endpoint '$operationName' already declares a headers binding."
        }
        headersBinding = DotnetAspHeadersBindingBuilder(name).apply(block).build()
    }

    override fun body(modelName: String) {
        require(bodyBinding == null) {
            "ASP.NET endpoint '$operationName' already declares a body binding."
        }
        bodyBinding = DotnetAspModelReference.Shared(modelName)
    }

    override fun body(name: String, block: DotnetModelScope.() -> Unit) {
        require(bodyBinding == null) {
            "ASP.NET endpoint '$operationName' already declares a body binding."
        }
        bodyBinding = DotnetAspModelReference.Inline(
            InlineDotnetModelBuilder(name).apply(block).build(),
        )
    }

    override fun responses(block: DotnetAspResponsesScope.() -> Unit) {
        require(responses.isEmpty()) {
            "ASP.NET endpoint '$operationName' already declares responses."
        }
        responses = DotnetAspResponsesBuilder().apply(block).build()
    }

    fun build() = DotnetAspEndpoint(
        method = method,
        path = path,
        operationName = operationName,
        bindings = DotnetAspEndpointBindings(
            path = pathBinding,
            query = queryBinding,
            headers = headersBinding,
            body = bodyBinding,
        ),
        responses = responses,
    )
}
