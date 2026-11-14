package io.github.lmliam.microsmith.cli.plugins.remote

import com.sun.net.httpserver.HttpServer
import io.github.lmliam.microsmith.cli.command.RunCommand
import io.github.lmliam.microsmith.cli.plugins.PluginResolutionResult
import io.github.lmliam.microsmith.cli.plugins.PluginResolverSettings
import io.github.lmliam.microsmith.cli.plugins.fileRepositoryAllowedPolicy
import io.github.lmliam.microsmith.cli.plugins.publishMavenArtifact
import io.github.lmliam.microsmith.cli.plugins.resolvePlugins
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
class PluginResolverHttpResolutionTests :
    StringSpec(
        {
            "resolves plugin coordinate and transitive dependency from an http repository" {
                val tempDir = createTempDirectory("microsmith-plugin-resolver-http")
                val server = serveDirectory(tempDir.resolve("repo"))
                try {
                    val repositoryRoot = tempDir.resolve("repo")
                    val rootCoordinate = "com.acme:plugin-root:1.0.0"
                    val transitiveCoordinate = "com.acme:plugin-shared:2.1.0"
                    publishMavenArtifact(repositoryRoot, transitiveCoordinate)
                    publishMavenArtifact(repositoryRoot, rootCoordinate, dependencies = listOf(transitiveCoordinate))
                    val script = tempDir.resolve("schema.microsmith.kts")
                    script.writeText("// test script")
                    val repositoryUrl = "http://127.0.0.1:${server.address.port}/"

                    val result =
                        resolvePlugins(
                            command =
                                RunCommand(
                                    script = script,
                                    outputDir = tempDir.resolve("generated"),
                                    plugins = setOf(rootCoordinate),
                                    repositoryOverride = repositoryUrl,
                                ),
                            settings =
                                PluginResolverSettings(
                                    cacheDirectory = tempDir.resolve("cache"),
                                    repositoryPolicy = fileRepositoryAllowedPolicy(repositoryUrl),
                                ),
                        )

                    val success = result.shouldBeTypeOf<PluginResolutionResult.Success>()
                    success.classpath.shouldHaveSize(2)
                    success.classpath.all { path -> path.exists() } shouldBe true
                    success.classpath.map { path -> path.fileName.toString() }.toSet() shouldBe
                        setOf("plugin-root-1.0.0.jar", "plugin-shared-2.1.0.jar")
                } finally {
                    server.stop(0)
                    runCatching { tempDir.deleteRecursively() }
                }
            }
        },
    )

private fun serveDirectory(root: Path): HttpServer {
    Files.createDirectories(root)
    val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    server.createContext("/") { exchange ->
        val file = root.resolve(exchange.requestURI.path.removePrefix("/")).normalize()
        if (file.startsWith(root) && Files.isRegularFile(file)) {
            val bytes = Files.readAllBytes(file)
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        } else {
            exchange.sendResponseHeaders(404, -1)
        }
        exchange.close()
    }
    server.start()
    return server
}
