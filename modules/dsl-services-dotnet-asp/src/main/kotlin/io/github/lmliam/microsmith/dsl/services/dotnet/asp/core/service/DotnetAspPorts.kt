package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.service

data class DotnetAspPorts(
    val http: Int? = null,
    val https: Int? = null,
) {
    init {
        validatePort(http, "HTTP")
        validatePort(https, "HTTPS")
        require(http == null || https == null || http != https) {
            "ASP.NET HTTP and HTTPS ports must be distinct."
        }
    }
}

internal fun mergeDotnetAspPorts(left: DotnetAspPorts, right: DotnetAspPorts): DotnetAspPorts = DotnetAspPorts(
    http = mergeDotnetAspPortValue("HTTP", left.http, right.http),
    https = mergeDotnetAspPortValue("HTTPS", left.https, right.https),
)

private fun mergeDotnetAspPortValue(label: String, left: Int?, right: Int?): Int? = when {
    left == null -> right
    right == null -> left
    else -> error("ASP.NET service already declares an explicit $label port.")
}

private fun validatePort(port: Int?, label: String) {
    require(port == null || port in MIN_DOTNET_ASP_PORT..MAX_DOTNET_ASP_PORT) {
        "ASP.NET $label port must be between $MIN_DOTNET_ASP_PORT and $MAX_DOTNET_ASP_PORT."
    }
}

private const val MIN_DOTNET_ASP_PORT = 1
private const val MAX_DOTNET_ASP_PORT = 65_535
