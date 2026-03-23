package io.github.lmliam.microsmith.sbt

import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.internal.util.MessageOnlyException
import _root_.sbt.util.Logger

import scala.collection.JavaConverters._
import java.io.File
import java.nio.file.Files

object MicrosmithSbtPlugin extends AutoPlugin {
  override def requires: Plugins = plugins.JvmPlugin
  override def trigger: PluginTrigger = noTrigger

  object autoImport {
    val microsmithGenerate = TaskKey[Seq[File]](
      "microsmithGenerate",
      "Generates artifacts from a .microsmith.kts script.",
    )
    val microsmithScriptFile = SettingKey[File](
      "microsmithScriptFile",
      "Path to the .microsmith.kts script to execute.",
    )
    val microsmithOutputDirectory = SettingKey[File](
      "microsmithOutputDirectory",
      "Directory where generated outputs are written.",
    )
    val microsmithCacheDirectory = SettingKey[File](
      "microsmithCacheDirectory",
      "Directory used for Microsmith script compilation cache entries.",
    )
    val microsmithVariables = SettingKey[Map[String, String]](
      "microsmithVariables",
      "Variables exposed to the script via requireVar and hasVar.",
    )
    val microsmithFlags = SettingKey[Set[String]](
      "microsmithFlags",
      "Flags exposed to the script via hasFlag.",
    )
  }

  import autoImport._

  private val executionService = new MicrosmithSbtExecutionService()

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    microsmithScriptFile := new File(baseDirectory.value, "build.microsmith.kts"),
    microsmithOutputDirectory := baseDirectory.value,
    microsmithCacheDirectory := new File(new File(new File(target.value, "tmp"), "microsmith"), "cache"),
    microsmithVariables := Map.empty,
    microsmithFlags := Set.empty,
    microsmithGenerate := generate(
      logger = streams.value.log,
      baseDirectory = baseDirectory.value,
      scriptFile = microsmithScriptFile.value,
      outputDirectory = microsmithOutputDirectory.value,
      cacheDirectory = microsmithCacheDirectory.value,
      variables = microsmithVariables.value,
      flags = microsmithFlags.value,
    ),
  )

  private def generate(
    logger: Logger,
    baseDirectory: File,
    scriptFile: File,
    outputDirectory: File,
    cacheDirectory: File,
    variables: Map[String, String],
    flags: Set[String],
  ): Seq[File] = {
    val configuration = new MicrosmithSbtExecutionConfiguration(
      baseDirectory.toPath,
      scriptFile.toPath,
      outputDirectory.toPath,
      cacheDirectory.toPath,
      variables.asJava,
      flags.asJava,
    )

    try {
      val outcome = executionService.execute(configuration)
      outcome.getWarnings.asScala.foreach(message => logger.warn(message))
      val generatedOutputRoot = outcome.getOutputDirectory.toAbsolutePath.normalize.resolve("proto")
      logger.info(
        s"Generated Microsmith outputs into '$generatedOutputRoot'. " +
          s"(compile-cache=${if (outcome.getCacheHit) "hit" else "miss"}, elapsed=${outcome.getElapsedMillis}ms)"
      )
      generatedFiles(generatedOutputRoot.toFile)
    } catch {
      case error: MicrosmithSbtScriptFailureException =>
        throw new MessageOnlyException(error.getMessage)
      case error: MicrosmithSbtHostFailureException =>
        throw new IllegalStateException(error.getMessage, error)
    }
  }

  private def generatedFiles(outputDirectory: File): Seq[File] = {
    if (!outputDirectory.isDirectory) {
      return Seq.empty
    }

    val stream = Files.walk(outputDirectory.toPath)
    try {
      stream.iterator.asScala
        .filter(Files.isRegularFile(_))
        .map(_.toFile)
        .toVector
        .sortBy(_.getAbsolutePath)
    } finally {
      stream.close()
    }
  }
}
