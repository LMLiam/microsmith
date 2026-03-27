package io.github.lmliam.microsmith.resolve.services.dotnet

internal object DotnetWorkspaceResolutionErrors {
    fun solutionNotDeclared(serviceName: String, solutionName: String): IllegalStateException {
        return IllegalStateException(
            "Dotnet solution '$solutionName' is not declared for service '$serviceName'.",
        )
    }
}
