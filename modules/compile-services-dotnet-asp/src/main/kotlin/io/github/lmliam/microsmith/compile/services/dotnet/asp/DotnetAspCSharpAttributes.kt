package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import java.util.Locale

internal object DotnetAspCSharpAttributes {
    object Microsoft {
        object AspNetCore {
            object Mvc {
                val ApiController: CSharp.Attribute = CSharp.attribute("ApiController")
                val FromBody: CSharp.Attribute = CSharp.attribute("FromBody")
                val FromQuery: CSharp.Attribute = CSharp.attribute("FromQuery")
                val FromRoute: CSharp.Attribute = CSharp.attribute("FromRoute")

                fun endpointRoute(method: String, route: String, operationName: String): CSharp.Attribute =
                    CSharp.attribute(
                        name = httpMethodAttributeName(method),
                        CSharp.positionalArgument(CSharp.stringLiteral(route)),
                        CSharp.namedArgument("Name", CSharp.stringLiteral(operationName)),
                    )

                fun producesResponseType(typeName: String, statusCode: Int): CSharp.Attribute = CSharp.attribute(
                    name = "ProducesResponseType",
                    CSharp.positionalArgument(CSharp.rawExpression("typeof($typeName)")),
                    CSharp.positionalArgument(CSharp.intLiteral(statusCode)),
                )
            }
        }
    }
}

private fun httpMethodAttributeName(method: String): String =
    "Http" + method.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase)
