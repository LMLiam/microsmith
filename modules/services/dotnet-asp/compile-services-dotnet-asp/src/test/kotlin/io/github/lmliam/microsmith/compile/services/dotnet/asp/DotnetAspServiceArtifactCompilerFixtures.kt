package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.contract.DotnetAspContractFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.contract.DotnetAspContractTypeRendering
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.DotnetAspControllerActionRendering
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.DotnetAspControllerFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.DotnetAspControllerResultRendering
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.hosting.DotnetAspInfrastructureFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpRenderer
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.DotnetAspNamingPolicy

internal fun dotnetAspServiceArtifactCompiler(): DotnetAspServiceArtifactCompiler {
    val csharpRenderer = CSharpRenderer()
    val namingPolicy = DotnetAspNamingPolicy()
    return DotnetAspServiceArtifactCompiler(
        DotnetAspInfrastructureFileRenderer(csharpRenderer),
        DotnetAspContractFileRenderer(csharpRenderer, DotnetAspContractTypeRendering(namingPolicy)),
        DotnetAspControllerFileRenderer(
            csharpRenderer,
            namingPolicy,
            DotnetAspControllerActionRendering(namingPolicy),
            DotnetAspControllerResultRendering(namingPolicy),
        ),
        namingPolicy,
    )
}
