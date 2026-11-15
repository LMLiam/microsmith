package io.github.lmliam.microsmith.artifact.services.dotnet

import io.github.lmliam.microsmith.artifact.files.RelativePath

private val dotnetOutputRoot = RelativePath.of("dotnet")

public fun dotnetSolutionOutputRoot(solutionName: String): RelativePath = dotnetOutputRoot.resolve(solutionName)

public fun dotnetProjectOutputRoot(solutionName: String, projectName: String): RelativePath =
    dotnetOutputRoot.resolve(solutionName, projectName)
