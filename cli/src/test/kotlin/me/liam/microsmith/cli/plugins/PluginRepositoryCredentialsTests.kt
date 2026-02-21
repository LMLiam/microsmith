package me.liam.microsmith.cli.plugins

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
                val credentialsFile = tempDir.resolve("repository-credentials.txt")
                credentialsFile.writeText(
                    """
                    https://repo1.maven.org/maven2|file-user|file-secret
                    """.trimIndent(),
                )

                val resolver =
                    defaultRepositoryCredentialsResolver(
                        repositoryCredentialsFileEnv = credentialsFile.toString(),
                        repositoryUsernameEnv = "global-user",
                        repositoryPasswordEnv = "global-secret",
                    )

                resolver.resolve("https://repo1.maven.org/maven2") shouldBe
                    RepositoryCredentials(username = "file-user", password = "file-secret")
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }

        "github packages credentials use github actor fallback and only apply to github packages host" {
            val resolver =
                defaultRepositoryCredentialsResolver(
                    githubPackagesUsernameEnv = null,
                    githubPackagesTokenEnv = null,
                    githubActorEnv = "octocat",
                    githubTokenEnv = "gh-token",
                )

            resolver.resolve("https://maven.pkg.github.com/acme/microsmith") shouldBe
                RepositoryCredentials(username = "octocat", password = "gh-token")
            resolver.resolve("https://repo1.maven.org/maven2") shouldBe null
        }

        "global repository credentials are used for non-github repositories" {
            val resolver =
                defaultRepositoryCredentialsResolver(
                    repositoryUsernameEnv = "ci-user",
                    repositoryPasswordEnv = "ci-pass",
                )

            resolver.resolve("https://packages.acme.internal/maven") shouldBe
                RepositoryCredentials(username = "ci-user", password = "ci-pass")
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
                val credentialsFile = tempDir.resolve("repository-credentials.txt")
                credentialsFile.writeText(
                    """
                    https://repo1.maven.org/maven2|first|secret-one
                    https://repo1.maven.org/maven2|second|secret-two
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
                val credentialsFile = tempDir.resolve("repository-credentials.txt")
                credentialsFile.writeText(
                    """
                    https://repo1.maven.org/maven2|file-user|file-secret
                    """.trimIndent(),
                )

                val resolver =
                    defaultRepositoryCredentialsResolver(
                        repositoryCredentialsFileEnv = credentialsFile.toString(),
                        repositoryUsernameEnv = "global-user",
                        repositoryPasswordEnv = "global-secret",
                        githubPackagesUsernameEnv = "gh-user",
                        githubPackagesTokenEnv = "gh-secret",
                    )

                val sensitiveValues = resolver.sensitiveValues()
                sensitiveValues shouldContain "file-secret"
                sensitiveValues shouldContain "global-secret"
                sensitiveValues shouldContain "gh-secret"
                sensitiveValues.contains("gh-user") shouldBe false
                sensitiveValues.contains("global-user") shouldBe false
            } finally {
                runCatching { tempDir.deleteRecursively() }
            }
        }
    })
