package io.github.lmliam.microsmith.gen.services.dotnet.packages.emission

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.defaults.DotnetDefaultsExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.github.lmliam.microsmith.gen.services.ServiceEmitter
import io.github.lmliam.microsmith.gen.services.dotnet.packages.DotnetPackageWorkspaceResolver
import io.github.lmliam.microsmith.gen.services.dotnet.packages.ResolvedDotnetPackageSolution
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.reflect.KClass

@ServiceProvider(ServiceEmitter::class)
class DotnetPackageVersionsEmitter : ServiceEmitter<DotnetDefaultsExtension> {
    private val workspaceResolver = DotnetPackageWorkspaceResolver()

    override val type: KClass<DotnetDefaultsExtension> = DotnetDefaultsExtension::class

    override suspend fun DotnetDefaultsExtension.emit(
        services: ServicesExtension,
        space: FileSpace,
    ): List<GeneratedFile> {
        val workspace = workspaceResolver.resolve(services)
        return workspace.solutions.values.map { solution ->
            GeneratedFile(
                relativePath = Path.of("Directory.Packages.props"),
                contents = buildDirectoryPackagesProps(solution).toByteArray(StandardCharsets.UTF_8),
                outputRoot = Path.of("dotnet", solution.name),
            )
        }
    }

    private fun buildDirectoryPackagesProps(solution: ResolvedDotnetPackageSolution): String {
        return buildString {
            appendLine("<Project>")
            appendLine("  <PropertyGroup>")
            appendLine("    <ManagePackageVersionsCentrally>true</ManagePackageVersionsCentrally>")
            appendLine("  </PropertyGroup>")
            appendLine("  <ItemGroup>")
            solution.packages.forEach { (packageName, version) ->
                appendLine(
                    "    <PackageVersion Include=\"${xmlEscape(packageName)}\" " +
                        "Version=\"${xmlEscape(version)}\" />",
                )
            }
            appendLine("  </ItemGroup>")
            appendLine("</Project>")
        }
    }
}
