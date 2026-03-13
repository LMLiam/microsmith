package io.github.lmliam.microsmith.cli.plugins

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginRepositoryCredentialsTests :
    StringSpec({
        "repository credentials file entries take precedence over global credentials" {
            val tempDir = createTempDirectory("microsmith-plugin-credentials-precedence")
            try {
                val fileCredential = "example-file-credential"
                val globalCredential = "example-global-credential"
                val credentialsFile = tempDir.resolve("repository-credentials.txt")
                credentialsFile.writeText(
                    """
                    https://repo1.maven.org/maven2|file-user|$fileCredential
                    """.trimIndent(),
                )

                val resolver =
                    defaultRepositoryCredentialsResolver(
                        repositoryCredentialsFileEnv = credentialsFile.toString(),
                        repositoryUsernameEnv = "global-user",
                        repositoryPasswordEnv = globalCredential,
                    )

                resolver.resolve("https://repo1.maven.org/maven2") shouldBe
                    RepositoryCredentials(username = "file-user", password = fileCredential)
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "github packages credentials use github actor fallback and only apply to github packages host" {
            val githubCredential = "example-github-credential"
            val resolver =
                defaultRepositoryCredentialsResolver(
                    githubPackagesUsernameEnv = null,
                    githubPackagesTokenEnv = null,
                    githubActorEnv = "octocat",
                    githubTokenEnv = githubCredential,
                )

            resolver.resolve("https://maven.pkg.github.com/acme/microsmith") shouldBe
                RepositoryCredentials(username = "octocat", password = githubCredential)
            resolver.resolve("https://repo1.maven.org/maven2") shouldBe null
        }

        "global repository credentials are used for non-github repositories" {
            val globalCredential = "example-global-credential"
            val resolver =
                defaultRepositoryCredentialsResolver(
                    repositoryUsernameEnv = "ci-user",
                    repositoryPasswordEnv = globalCredential,
                )

            resolver.resolve("https://packages.acme.internal/maven") shouldBe
                RepositoryCredentials(username = "ci-user", password = globalCredential)
        }

        "fails when only one global credential environment variable is configured" {
            shouldThrow<IllegalArgumentException> {
                defaultRepositoryCredentialsResolver(
                    repositoryUsernameEnv = "ci-user",
                    repositoryPasswordEnv = null,
                )
            }.message.shouldContain("Set both MICROSMITH_REPOSITORY_USERNAME and MICROSMITH_REPOSITORY_PASSWORD")
        }

        "fails when repository credentials file has duplicate repository entries" {
            val tempDir = createTempDirectory("microsmith-plugin-credentials-duplicate")
            try {
                val firstCredential = "example-first-credential"
                val secondCredential = "example-second-credential"
                val credentialsFile = tempDir.resolve("repository-credentials.txt")
                credentialsFile.writeText(
                    """
                    https://repo1.maven.org/maven2|first|$firstCredential
                    https://repo1.maven.org/maven2|second|$secondCredential
                    """.trimIndent(),
                )

                shouldThrow<IllegalArgumentException> {
                    defaultRepositoryCredentialsResolver(
                        repositoryCredentialsFileEnv = credentialsFile.toString(),
                    )
                }.message.shouldContain("Duplicate repository credentials entry")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "sensitive values includes secrets but excludes usernames" {
            val tempDir = createTempDirectory("microsmith-plugin-credentials-sensitive-values")
            try {
                val fileCredential = "example-file-credential"
                val globalCredential = "example-global-credential"
                val githubCredential = "example-github-credential"
                val credentialsFile = tempDir.resolve("repository-credentials.txt")
                credentialsFile.writeText(
                    """
                    https://repo1.maven.org/maven2|file-user|$fileCredential
                    """.trimIndent(),
                )

                val resolver =
                    defaultRepositoryCredentialsResolver(
                        repositoryCredentialsFileEnv = credentialsFile.toString(),
                        repositoryUsernameEnv = "global-user",
                        repositoryPasswordEnv = globalCredential,
                        githubPackagesUsernameEnv = "gh-user",
                        githubPackagesTokenEnv = githubCredential,
                    )

                val sensitiveValues = resolver.sensitiveValues()
                sensitiveValues shouldContain fileCredential
                sensitiveValues shouldContain globalCredential
                sensitiveValues shouldContain githubCredential
                sensitiveValues.contains("gh-user") shouldBe false
                sensitiveValues.contains("global-user") shouldBe false
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "redacts overlapping secrets using longest-first replacement" {
            val sanitized =
                "token=alphabet".redactSensitiveValues(
                    setOf("alpha", "alphabet"),
                )

            sanitized shouldBe "token=<redacted>"
        }
    })
