package io.github.lmliam.microsmith.gen.services.dotnet

internal object DotnetGenerationErrors {
    fun solutionNotDeclared(serviceName: String, solutionName: String): IllegalStateException {
        return IllegalStateException(
            "Dotnet solution '$solutionName' is not declared for service '$serviceName'.",
        )
    }
}
