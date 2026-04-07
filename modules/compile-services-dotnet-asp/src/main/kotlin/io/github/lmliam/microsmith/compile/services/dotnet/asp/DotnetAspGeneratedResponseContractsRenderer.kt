package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.DotnetCSharpTypes
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpNullableType
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.csharpType
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint

internal fun renderOperationResultTypes(endpoint: ResolvedDotnetAspEndpoint): List<CSharp.Type> = buildList {
    add(
        CSharp.Type(
            kind = CSharp.TypeKind.RECORD,
            name = resultBaseTypeName(endpoint),
            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
        ),
    )
    endpoint.responses.forEach { response ->
        add(
            CSharp.Type(
                kind = CSharp.TypeKind.RECORD,
                name = resultVariantTypeName(endpoint, response),
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                baseTypes = listOf(csharpType(resultBaseTypeName(endpoint))),
                primaryConstructorParameters = buildList {
                    add(
                        CSharp.Parameter(
                            type = csharpType(resolveResponseModelTypeName(endpoint, response)),
                            name = "Body",
                        ),
                    )
                    response.headers.forEach { header ->
                        add(
                            CSharp.Parameter(
                                type = csharpNullableType(DotnetCSharpTypes.Primitives.String),
                                name = dotnetAspHeaderPropertyName(header.name),
                                defaultValue = "null",
                            ),
                        )
                    }
                },
            ),
        )
    }
}
