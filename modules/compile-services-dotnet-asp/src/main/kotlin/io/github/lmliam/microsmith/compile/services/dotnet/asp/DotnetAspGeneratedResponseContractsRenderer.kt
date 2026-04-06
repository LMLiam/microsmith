package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharp
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint

internal fun renderOperationResultTypes(endpoint: ResolvedDotnetAspEndpoint): List<CSharp.Type> = buildList {
    add(
        CSharp.Type(
            kind = CSharp.TypeKind.RECORD,
            name = resultBaseTypeName(endpoint),
            modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.ABSTRACT),
            baseTypes = emptyList(),
            attributes = emptyList(),
            primaryConstructorParameters = emptyList(),
            members = emptyList(),
        ),
    )
    endpoint.responses.forEach { response ->
        add(
            CSharp.Type(
                kind = CSharp.TypeKind.RECORD,
                name = resultVariantTypeName(endpoint, response),
                modifiers = listOf(CSharp.Modifier.PUBLIC, CSharp.Modifier.SEALED),
                baseTypes = listOf(csharpType(resultBaseTypeName(endpoint))),
                attributes = emptyList(),
                primaryConstructorParameters = buildList {
                    add(
                        CSharp.Parameter(
                            type = csharpType(resolveResponseModelTypeName(endpoint, response)),
                            name = "Body",
                            modifiers = emptyList(),
                            attributes = emptyList(),
                            defaultValue = null,
                        ),
                    )
                    response.headers.forEach { header ->
                        add(
                            CSharp.Parameter(
                                type = csharpNullableType(DotnetAspCSharpTypes.Primitives.String),
                                name = dotnetAspHeaderPropertyName(header.name),
                                modifiers = emptyList(),
                                attributes = emptyList(),
                                defaultValue = "null",
                            ),
                        )
                    }
                },
                members = emptyList(),
            ),
        )
    }
}
