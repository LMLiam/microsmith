package me.liam.microsmith.cli.plugins

import me.liam.microsmith.cli.command.RunCommand
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.name

private const val HTTP_STATUS_OK = 200
private val HTTP_CLIENT: HttpClient = HttpClient.newHttpClient()

internal fun resolveRepositories(command: RunCommand, settings: PluginResolverSettings): List<String> {
    return resolveRepositories(command, settings, settings.repositoryPolicy ?: defaultRepositoryAllowlistPolicy())
}

internal fun resolveRepositories(
    command: RunCommand,
    settings: PluginResolverSettings,
    repositoryPolicy: RepositoryAllowlistPolicy,
): List<String> {
    val override = command.repositoryOverride?.trim()?.takeIf { it.isNotEmpty() }
    val repositories = (listOfNotNull(override) + settings.defaultRepositories).distinct()
    repositories.forEach(repositoryPolicy::validate)
    return repositories
}

internal fun resolveRemoteArtifact(
    coordinate: Coordinate,
    repositories: List<String>,
    cacheDirectory: Path,
    offline: Boolean,
): Path {
    val cachePath = cachePathFor(cacheDirectory, coordinate)
    if (Files.exists(cachePath)) {
        return cachePath
    }

    require(!offline) {
        "Offline mode is enabled and plugin '${coordinate.value}' is not in cache at '$cachePath'. " +
            "Run once without --offline to populate the cache."
    }

    val attemptedUris = mutableListOf<String>()
    repositories.forEach { repository ->
        val artifactUri = repositoryArtifactUri(repository, coordinate.relativeJarPath)
        attemptedUris += artifactUri
        val downloaded =
            runCatching {
                downloadArtifact(artifactUri, cachePath)
            }.getOrElse { error ->
                if (error is InterruptedException) {
                    Thread.currentThread().interrupt()
                }
                false
            }
        if (downloaded) {
            return cachePath
        }
    }

    error("Could not resolve plugin '${coordinate.value}'. Tried: ${attemptedUris.joinToString(", ")}.")
}

private fun cachePathFor(cacheDirectory: Path, coordinate: Coordinate): Path {
    val cacheRoot = cacheDirectory.resolve("artifacts").toAbsolutePath().normalize()
    val artifactPath =
        cacheRoot
            .resolve(coordinate.group.replace('.', '/'))
            .resolve(coordinate.artifact)
            .resolve(coordinate.version)
            .resolve("${coordinate.artifact}-${coordinate.version}.jar")
            .normalize()
    require(artifactPath.startsWith(cacheRoot)) {
        "Plugin coordinate '${coordinate.value}' resolves outside plugin cache directory."
    }
    return artifactPath
}

private fun repositoryArtifactUri(repository: String, relativePath: String): String =
    "${repository.trimEnd('/')}/$relativePath"

private fun downloadArtifact(artifactUri: String, destination: Path): Boolean {
    val uri = URI.create(artifactUri)
    return when (uri.scheme?.lowercase()) {
        "file" -> copyFileRepositoryArtifact(uri, destination)
        "http", "https" -> copyHttpRepositoryArtifact(uri, destination)
        else -> false
    }
}

private fun copyFileRepositoryArtifact(artifactUri: URI, destination: Path): Boolean {
    val source = Path.of(artifactUri)
    if (!Files.exists(source) || !Files.isRegularFile(source)) {
        return false
    }

    copyArtifactToCache(source, destination)
    return true
}

private fun copyHttpRepositoryArtifact(artifactUri: URI, destination: Path): Boolean {
    val response: HttpResponse<ByteArray>? =
        try {
            val request = HttpRequest.newBuilder(artifactUri).GET().build()
            HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray())
        } catch (_: IOException) {
            null
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        }

    val downloaded =
        if (response == null || response.statusCode() != HTTP_STATUS_OK) {
            false
        } else {
            Files.createDirectories(destination.parent)
            val tempPath = destination.resolveSibling("${destination.name}.part")
            Files.write(tempPath, response.body())
            Files.move(tempPath, destination, StandardCopyOption.REPLACE_EXISTING)
            true
        }

    if (!downloaded) {
        val tempPath = destination.resolveSibling("${destination.name}.part")
        Files.deleteIfExists(tempPath)
    }
    return downloaded
}

private fun copyArtifactToCache(source: Path, destination: Path) {
    Files.createDirectories(destination.parent)
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING)
}
