package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspHeadersBindingScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request.DotnetAspRequestBindingScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response.DotnetAspResponsesScope
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModelScope

@MicrosmithDsl
public interface DotnetAspEndpointScope {
    public fun path(name: String, block: DotnetAspRequestBindingScope.() -> Unit)

    public fun query(name: String, block: DotnetAspRequestBindingScope.() -> Unit)

    public fun headers(name: String, block: DotnetAspHeadersBindingScope.() -> Unit)

    public fun body(modelName: String)

    public fun body(name: String, block: DotnetModelScope.() -> Unit)

    public fun responses(block: DotnetAspResponsesScope.() -> Unit)
}
