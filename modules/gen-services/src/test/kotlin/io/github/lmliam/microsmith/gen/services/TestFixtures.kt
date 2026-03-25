package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.core.MicrosmithExtension
import io.github.lmliam.microsmith.dsl.services.core.Service
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.io.path.Path
import kotlin.reflect.KClass

private typealias SE = ServicesExtension
private typealias FS = FileSpace

internal class TestSharedEmitter : ServiceEmitter<TestSharedExtension> {
    override val type: KClass<TestSharedExtension> = TestSharedExtension::class

    override suspend fun TestSharedExtension.emit(services: SE, space: FS): List<GeneratedFile> {
        return listOf(GeneratedFile(Path("shared.out"), byteArrayOf()))
    }
}

internal class AdditionalTestSharedEmitter : ServiceEmitter<TestSharedExtension> {
    override val type: KClass<TestSharedExtension> = TestSharedExtension::class

    override suspend fun TestSharedExtension.emit(services: SE, space: FS): List<GeneratedFile> {
        return listOf(GeneratedFile(Path("additional-shared.out"), byteArrayOf()))
    }
}

internal class TestServiceEmitter : ServiceEmitter<TestServiceExtension> {
    override val type: KClass<TestServiceExtension> = TestServiceExtension::class

    override suspend fun TestServiceExtension.emit(service: Service, services: SE, space: FS): List<GeneratedFile> {
        return listOf(GeneratedFile(Path("${service.name}.out"), byteArrayOf()))
    }
}

internal class AdditionalTestServiceEmitter : ServiceEmitter<TestServiceExtension> {
    override val type: KClass<TestServiceExtension> = TestServiceExtension::class

    override suspend fun TestServiceExtension.emit(service: Service, services: SE, space: FS): List<GeneratedFile> {
        return listOf(GeneratedFile(Path("additional-${service.name}.out"), byteArrayOf()))
    }
}

internal data class TestSharedExtension(
    val value: String = "shared",
) : MicrosmithExtension

internal data class TestServiceExtension(
    val value: String = "test",
) : ServiceExtension
