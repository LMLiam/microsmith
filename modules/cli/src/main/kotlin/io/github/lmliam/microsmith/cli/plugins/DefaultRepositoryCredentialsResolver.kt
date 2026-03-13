package io.github.lmliam.microsmith.cli.plugins

import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

internal const val REPOSITORY_CREDENTIALS_FILE_ENV = "MICROSMITH_REPOSITORY_CREDENTIALS_FILE"
internal const val REPOSITORY_USERNAME_ENV = "MICROSMITH_REPOSITORY_USERNAME"
internal const val REPOSITORY_PASSWORD_ENV = "MICROSMITH_REPOSITORY_PASSWORD"
internal const val GITHUB_PACKAGES_USERNAME_ENV = "MICROSMITH_GITHUB_PACKAGES_USER"
internal const val GITHUB_PACKAGES_TOKEN_ENV = "MICROSMITH_GITHUB_PACKAGES_TOKEN"
internal const val GITHUB_ACTOR_ENV = "GITHUB_ACTOR"
internal const val GITHUB_TOKEN_ENV = "GITHUB_TOKEN"

private const val CREDENTIALS_FILE_ENTRY_PARTS = 3
private const val DEFAULT_GITHUB_PACKAGES_USERNAME = "x-access-token"
private const val GITHUB_PACKAGES_HOST = "maven.pkg.github.com"

internal class DefaultRepositoryCredentialsResolver(
    private val fileCredentialsByRepository: Map<String, RepositoryCredentials>,
    private val githubPackagesCredentials: RepositoryCredentials?,
    private val defaultCredentials: RepositoryCredentials?,
) : RepositoryCredentialsResolver {
    override fun resolve(repositoryUri: String): RepositoryCredentials? {
        val normalizedRepository = normalizeRepositoryUri(repositoryUri)
        return fileCredentialsByRepository[normalizedRepository]
            ?: githubPackagesCredentialsFor(normalizedRepository)
            ?: defaultCredentials
    }

    override fun sensitiveValues(): Set<String> = buildSet {
        fileCredentialsByRepository.values.mapTo(this) { credentials -> credentials.password }
        githubPackagesCredentials?.password?.let(this::add)
        defaultCredentials?.password?.let(this::add)
    }.filter(String::isNotBlank).toSet()

    private fun githubPackagesCredentialsFor(repositoryUri: String): RepositoryCredentials? {
        val host = URI.create(repositoryUri).host?.lowercase()
        return githubPackagesCredentials.takeIf { host == GITHUB_PACKAGES_HOST }
    }
}

internal fun defaultRepositoryCredentialsResolver(
    repositoryCredentialsFileEnv: String? = System.getenv(REPOSITORY_CREDENTIALS_FILE_ENV),
    repositoryUsernameEnv: String? = System.getenv(REPOSITORY_USERNAME_ENV),
    repositoryPasswordEnv: String? = System.getenv(REPOSITORY_PASSWORD_ENV),
    githubPackagesUsernameEnv: String? = System.getenv(GITHUB_PACKAGES_USERNAME_ENV),
    githubPackagesTokenEnv: String? = System.getenv(GITHUB_PACKAGES_TOKEN_ENV),
    githubActorEnv: String? = System.getenv(GITHUB_ACTOR_ENV),
    githubTokenEnv: String? = System.getenv(GITHUB_TOKEN_ENV),
): RepositoryCredentialsResolver {
    val credentialsFilePath = repositoryCredentialsFileEnv?.trim()?.takeIf(String::isNotEmpty)?.let(Path::of)
    val fileCredentials = credentialsFilePath?.let(::readRepositoryCredentialsFile).orEmpty()

    val repositoryUsername = repositoryUsernameEnv?.trim().orEmpty()
    val repositoryPassword = repositoryPasswordEnv?.trim().orEmpty()
    require(repositoryUsername.isNotEmpty() == repositoryPassword.isNotEmpty()) {
        "Set both $REPOSITORY_USERNAME_ENV and $REPOSITORY_PASSWORD_ENV, or leave both unset."
    }
    val defaultCredentials = repositoryUsername.takeIf(String::isNotEmpty)?.let { username ->
        RepositoryCredentials(username = username, password = repositoryPassword)
    }

    val githubPackagesToken =
        githubPackagesTokenEnv
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: githubTokenEnv
                ?.trim()
                ?.takeIf(String::isNotEmpty)
    val githubPackagesCredentials = githubPackagesToken?.let { token ->
        val githubPackagesUsername = githubPackagesUsernameEnv?.trim()?.takeIf(String::isNotEmpty)
        val githubActorUsername = githubActorEnv?.trim()?.takeIf(String::isNotEmpty)
        val username =
            githubPackagesUsername
                ?: githubActorUsername
                ?: DEFAULT_GITHUB_PACKAGES_USERNAME
        RepositoryCredentials(username = username, password = token)
    }

    return DefaultRepositoryCredentialsResolver(
        fileCredentialsByRepository = fileCredentials,
        githubPackagesCredentials = githubPackagesCredentials,
        defaultCredentials = defaultCredentials,
    )
}

private fun readRepositoryCredentialsFile(credentialsFilePath: Path): Map<String, RepositoryCredentials> {
    require(Files.exists(credentialsFilePath) && Files.isRegularFile(credentialsFilePath)) {
        "Repository credentials file '$credentialsFilePath' does not exist or is not a regular file."
    }

    val credentialsByRepository = linkedMapOf<String, RepositoryCredentials>()
    Files.readAllLines(credentialsFilePath).forEachIndexed { lineIndex, rawLine ->
        val lineNumber = lineIndex + 1
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) {
            return@forEachIndexed
        }

        val parts = line.split('|', limit = CREDENTIALS_FILE_ENTRY_PARTS)
        require(parts.size == CREDENTIALS_FILE_ENTRY_PARTS) {
            "Invalid repository credentials entry at '$credentialsFilePath:$lineNumber'. " +
                "Expected <repository-uri>|<username>|<password>."
        }

        val repositoryUri = normalizeRepositoryUri(parts[0])
        val username = parts[1].trim()
        val password = parts[2].trim()
        require(username.isNotEmpty() && password.isNotEmpty()) {
            "Invalid repository credentials entry at '$credentialsFilePath:$lineNumber'. " +
                "Username and password must both be non-empty."
        }
        require(!credentialsByRepository.containsKey(repositoryUri)) {
            "Duplicate repository credentials entry for '$repositoryUri' in '$credentialsFilePath'."
        }
        credentialsByRepository[repositoryUri] = RepositoryCredentials(username = username, password = password)
    }

    return credentialsByRepository.toMap()
}
