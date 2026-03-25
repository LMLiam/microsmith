package io.github.lmliam.microsmith.gen.services.dotnet.packages

internal object DotnetPackageWorkspaceResolutionErrors {
    fun packageNotDeclared(serviceName: String, solutionName: String, packageName: String): Nothing {
        error(
            "Dotnet service '$serviceName' references package '$packageName' " +
                "but solution '$solutionName' does not centrally own it.",
        )
    }
}
