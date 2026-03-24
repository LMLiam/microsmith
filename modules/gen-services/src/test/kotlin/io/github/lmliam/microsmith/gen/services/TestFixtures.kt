package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.services.core.Service
import io.github.lmliam.microsmith.dsl.services.core.ServiceExtension
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.files.GeneratedFile
import kotlin.io.path.Path
import kotlin.reflect.KClass

internal class TestServiceEmitter : ServiceEmitter<TestServiceExtension> {
    override val type: KClass<TestServiceExtension> = TestServiceExtension::class

    override suspend fun TestServiceExtension.emit(service: Service, space: FileSpace): List<GeneratedFile> =
        listOf(GeneratedFile(Path("${service.name}.out"), byteArrayOf()))
}

internal class DuplicateTestServiceEmitter : ServiceEmitter<TestServiceExtension> {
    override val type: KClass<TestServiceExtension> = TestServiceExtension::class

    override suspend fun TestServiceExtension.emit(service: Service, space: FileSpace): List<GeneratedFile> =
        listOf(GeneratedFile(Path("duplicate.out"), byteArrayOf()))
}

internal data class TestServiceExtension(
    val value: String = "test",
) : ServiceExtension
