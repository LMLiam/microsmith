package io.github.lmliam.microsmith.artifact.services.dotnet

import io.github.lmliam.microsmith.artifact.files.RelativePath

private val dotnetOutputRoot = RelativePath.of("dotnet")

fun dotnetSolutionOutputRoot(solutionName: String): RelativePath = dotnetOutputRoot.resolve(solutionName)

fun dotnetProjectOutputRoot(solutionName: String, projectName: String): RelativePath =
    dotnetOutputRoot.resolve(solutionName, projectName)
