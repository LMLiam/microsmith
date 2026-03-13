package me.liam.microsmith.maven

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import java.io.File
import java.io.UncheckedIOException
import java.util.Properties

class MicrosmithGenerateMojo : AbstractMojo() {
    var projectBaseDirectory: File = File(".")
    var scriptFile: File = File("build.microsmith.kts")
    var outputDirectory: File = File("target/generated/microsmith")
    var cacheDirectory: File = File("target/tmp/microsmith/cache")
    var variables: Properties? = null
    var flags: List<String>? = null

    internal var requestFactory: MicrosmithMavenExecutionRequestFactory = MicrosmithMavenExecutionRequestFactory()
    internal var scriptHostRunner: MicrosmithScriptHostRunner = DefaultMicrosmithScriptHostRunner
    internal var resultHandler: MicrosmithMavenResultHandler = MicrosmithMavenResultHandler()

    override fun execute() {
        try {
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
        } catch (error: MojoFailureException) {
            throw error
        } catch (error: MojoExecutionException) {
            throw error
        } catch (error: IllegalArgumentException) {
            throw unexpectedExecutionFailure(error)
        } catch (error: IllegalStateException) {
            throw unexpectedExecutionFailure(error)
        } catch (error: SecurityException) {
            throw unexpectedExecutionFailure(error)
        } catch (error: UncheckedIOException) {
            throw MojoExecutionException("Microsmith Maven plugin failed before generation completed.", error)
        }
    }

    private fun unexpectedExecutionFailure(error: RuntimeException): MojoExecutionException =
        MojoExecutionException("Microsmith Maven plugin failed before generation completed.", error)
}
