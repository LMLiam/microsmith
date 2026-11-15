package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.contract.DotnetAspContractFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.controller.DotnetAspControllerFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.asp.render.hosting.DotnetAspInfrastructureFileRenderer
import io.github.lmliam.microsmith.compile.services.dotnet.csharp.CSharpRenderer

internal fun dotnetAspServiceArtifactCompiler(): DotnetAspServiceArtifactCompiler {
    val csharpRenderer = CSharpRenderer()
    return DotnetAspServiceArtifactCompiler(
        DotnetAspInfrastructureFileRenderer(csharpRenderer),
        DotnetAspContractFileRenderer(csharpRenderer),
        DotnetAspControllerFileRenderer(csharpRenderer),
    )
}
