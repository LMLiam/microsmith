package me.liam.microsmith.cli.plugins

import java.net.URI

private const val REPOSITORY_ALLOWLIST_ENV = "MICROSMITH_REPOSITORY_ALLOWLIST"
private const val ALLOW_FILE_REPOSITORIES_ENV = "MICROSMITH_ALLOW_FILE_REPOSITORIES"

internal data class RepositoryAllowlistPolicy(
    val allowedRepositories: Set<String>,
    val allowFileRepositories: Boolean = false,
) {
    fun validate(repositoryUri: String) {
        val normalized = normalizeRepositoryUri(repositoryUri)
        val parsed = URI.create(normalized)
        when (parsed.scheme) {
            "file" -> require(allowFileRepositories) {
                "Repository '$repositoryUri' is blocked by policy: file:// repositories are not allowed. " +
                    "Set $ALLOW_FILE_REPOSITORIES_ENV=true to explicitly enable file repositories."
            }
            "http", "https" ->
                require(allowedRepositories.contains(normalized)) {
                    "Repository '$repositoryUri' is not in the allowed repository allowlist. " +
                        "Configure $REPOSITORY_ALLOWLIST_ENV to permit additional endpoints."
                }
            else -> error("Unsupported repository URI '$repositoryUri'. Use https://, http://, or file://.")
        }
    }
}

internal fun defaultRepositoryAllowlistPolicy(
    repositoryAllowlistEnv: String? = System.getenv(REPOSITORY_ALLOWLIST_ENV),
    allowFileRepositoriesEnv: String? = System.getenv(ALLOW_FILE_REPOSITORIES_ENV),
): RepositoryAllowlistPolicy {
    val envAllowlist =
        repositoryAllowlistEnv
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()
    val allowFileRepositories = parseBooleanEnv(allowFileRepositoriesEnv)
    val allowedRepositories =
        (listOf(MAVEN_CENTRAL_REPOSITORY) + envAllowlist)
            .map(::normalizeRepositoryUri)
            .toSet()

    return RepositoryAllowlistPolicy(
        allowedRepositories = allowedRepositories,
        allowFileRepositories = allowFileRepositories,
    )
}

internal fun normalizeRepositoryUri(uri: String): String {
    val parsed = URI.create(uri.trim())
    val scheme = parsed.scheme?.lowercase()
    require(scheme == "https" || scheme == "http" || scheme == "file") {
        "Unsupported repository URI '$uri'. Use https://, http://, or file://."
    }
    require(parsed.query == null && parsed.fragment == null) {
        "Repository URI '$uri' must not include query parameters or fragments."
    }

    return when (scheme) {
        "file" -> {
            require(parsed.rawAuthority == null) {
                "Repository URI '$uri' must not include an authority/host for file:// scheme."
            }
            val path = parsed.path?.ifEmpty { "/" } ?: "/"
            URI("file", null, path, null).toString().trimEnd('/')
        }
        "http", "https" -> {
            require(parsed.userInfo == null) {
                "Repository URI '$uri' must not include userinfo credentials."
            }
            val host = parsed.host?.lowercase()
            require(!host.isNullOrBlank()) {
                "Repository URI '$uri' must include a valid host."
            }
            val path = parsed.path?.ifEmpty { "" }.orEmpty()
            URI(scheme, parsed.userInfo, host, parsed.port, path, null, null).toString().trimEnd('/')
        }
        else -> error("Unsupported repository URI '$uri'. Use https://, http://, or file://.")
    }
}

private fun parseBooleanEnv(value: String?): Boolean = value
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.equals("true", ignoreCase = true)
    ?: false
