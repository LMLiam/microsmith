package io.github.lmliam.microsmith.gen.services.dotnet.packages.emission

import com.github.eventhorizonlab.spi.ServiceProvider
import io.github.lmliam.microsmith.dsl.services.core.Service
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.dsl.services.dotnet.core.service.DotnetServiceExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import io.github.lmliam.microsmith.gen.services.ServiceEmitter
import io.github.lmliam.microsmith.gen.services.dotnet.packages.DotnetPackageWorkspaceResolver
import io.github.lmliam.microsmith.gen.services.dotnet.packages.ResolvedDotnetPackageService
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.reflect.KClass

@ServiceProvider(ServiceEmitter::class)
class DotnetPackageReferencesEmitter : ServiceEmitter<DotnetServiceExtension> {
    private val workspaceResolver = DotnetPackageWorkspaceResolver()

    override val type: KClass<DotnetServiceExtension> = DotnetServiceExtension::class

    override suspend fun DotnetServiceExtension.emit(
        service: Service,
        services: ServicesExtension,
        space: FileSpace,
    ): List<GeneratedFile> {
        val workspace = workspaceResolver.resolve(services)
        val resolvedService = workspace.services[service.name] ?: return emptyList()

        return listOf(resolvedService.toGeneratedFile())
    }

    private fun ResolvedDotnetPackageService.toGeneratedFile(): GeneratedFile {
        return GeneratedFile(
            relativePath = Path.of("PackageReferences.props"),
            contents = buildPackageReferencesProps().toByteArray(StandardCharsets.UTF_8),
            outputRoot = Path.of("dotnet", solution, project),
        )
    }

    private fun ResolvedDotnetPackageService.buildPackageReferencesProps(): String {
        return buildString {
            appendLine("<Project>")
            appendLine("  <ItemGroup>")
            packages.forEach { (packageName, _) ->
                appendLine("    <PackageReference Include=\"${xmlEscape(packageName)}\" />")
            }
            appendLine("  </ItemGroup>")
            appendLine("</Project>")
        }
    }
}
