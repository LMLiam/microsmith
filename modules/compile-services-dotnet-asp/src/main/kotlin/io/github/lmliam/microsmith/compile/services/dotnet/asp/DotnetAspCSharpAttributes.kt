package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspHttpMethod

internal object DotnetAspCSharpAttributes {
    object Microsoft {
        object AspNetCore {
            object Mvc {
                val ApiController: CSharp.Attribute = CSharp.attribute("ApiController")
                val FromBody: CSharp.Attribute = CSharp.attribute("FromBody")
                val FromQuery: CSharp.Attribute = CSharp.attribute("FromQuery")
                val FromRoute: CSharp.Attribute = CSharp.attribute("FromRoute")

                fun endpointRoute(
                    method: DotnetAspHttpMethod,
                    route: String,
                    operationName: String,
                ): CSharp.Attribute = CSharp.attribute(
                    name = httpMethodAttributeName(method),
                    CSharp.positionalArgument(CSharp.stringLiteral(route)),
                    CSharp.namedArgument("Name", CSharp.stringLiteral(operationName)),
                )
            }
        }
    }
}

private fun httpMethodAttributeName(method: DotnetAspHttpMethod): String = when (method) {
    DotnetAspHttpMethod.GET -> "HttpGet"
    DotnetAspHttpMethod.POST -> "HttpPost"
    DotnetAspHttpMethod.PUT -> "HttpPut"
    DotnetAspHttpMethod.PATCH -> "HttpPatch"
    DotnetAspHttpMethod.DELETE -> "HttpDelete"
}
