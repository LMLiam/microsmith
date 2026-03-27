package io.github.lmliam.microsmith.resolve.services.dotnet.packages

internal object DotnetPackageWorkspaceResolutionErrors {
    fun packageNotDeclared(serviceName: String, solutionName: String, packageName: String): Nothing {
        error(
            "Dotnet service '$serviceName' references package '$packageName' " +
                "but solution '$solutionName' does not centrally own it.",
        )
    }

    fun packageVersionRequired(serviceName: String, packageName: String): Nothing {
        error(
            "Dotnet service '$serviceName' references package '$packageName' " +
                "without a version and without central package ownership.",
        )
    }

    fun mixedPackageVersionManagement(serviceName: String, solutionName: String, packageName: String): Nothing {
        error(
            "Dotnet service '$serviceName' declares package '$packageName' with an explicit version " +
                "but solution '$solutionName' uses central package management. Mixed central and direct " +
                "package version management is not supported within the same solution.",
        )
    }
}
