package io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.attribute
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.intLiteral
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.namedArgument
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.positionalArgument
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.rawExpression
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.stringLiteral
import java.util.Locale

internal object DotnetAspCSharpAttributes {
    object Microsoft {
        object AspNetCore {
            object Mvc {
                val ApiController: CSharp.Attribute = attribute("ApiController")
                val FromBody: CSharp.Attribute = attribute("FromBody")
                val FromQuery: CSharp.Attribute = attribute("FromQuery")
                val FromRoute: CSharp.Attribute = attribute("FromRoute")

                fun endpointRoute(method: String, route: String, operationName: String): CSharp.Attribute =
                    attribute(
                        name = httpMethodAttributeName(method),
                        positionalArgument(stringLiteral(route)),
                        namedArgument("Name", stringLiteral(operationName)),
                    )

                fun producesResponseType(typeName: String, statusCode: Int): CSharp.Attribute =
                    attribute(
                        name = "ProducesResponseType",
                        positionalArgument(rawExpression("typeof($typeName)")),
                        positionalArgument(intLiteral(statusCode)),
                    )
            }
        }
    }
}

private fun httpMethodAttributeName(method: String): String =
    "Http" + method.lowercase(Locale.ROOT).replaceFirstChar(Char::uppercase)
