package io.github.lmliam.microsmith.maven

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import java.io.File
import java.util.Properties

class MicrosmithGenerateMojo : AbstractMojo() {
    var projectBaseDirectory: File = File(".")
    var scriptFile: File = File("build.microsmith.kts")
    var outputDirectory: File = File(".")
    var cacheDirectory: File = File("target/tmp/microsmith/cache")
    var variables: Properties? = null
    var flags: List<String>? = null

    internal var requestFactory: MicrosmithMavenExecutionRequestFactory = MicrosmithMavenExecutionRequestFactory()
    internal var scriptHostRunner: MicrosmithScriptHostRunner = DefaultMicrosmithScriptHostRunner
    internal var resultHandler: MicrosmithMavenResultHandler = MicrosmithMavenResultHandler()

    override fun execute() {
        runWithMojoFailureMapping {
            val request =
                requestFactory.create(
                    MicrosmithMavenExecutionConfiguration(
                        baseDirectory = projectBaseDirectory.toPath(),
                        scriptFile = scriptFile.toPath(),
                        outputDirectory = outputDirectory.toPath(),
                        cacheDirectory = cacheDirectory.toPath(),
                        variables = variables,
                        flags = flags,
                    ),
                )
            val result = scriptHostRunner.run(request.cacheDirectory, request.scriptRunRequest)
            resultHandler.handle(log, request.outputDirectory, result)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun runWithMojoFailureMapping(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            // Maven plugin entrypoints should not leak raw runtime failures into Maven core.
            throw error.toMojoException()
        }
    }

    private fun Exception.toMojoException(): Exception = when (this) {
        is MojoFailureException -> this
        is MojoExecutionException -> this
        is RuntimeException -> unexpectedExecutionFailure(this)
        else -> this
    }

    private fun unexpectedExecutionFailure(error: RuntimeException): MojoExecutionException =
        MojoExecutionException("Microsmith Maven plugin failed before generation completed.", error)
}
