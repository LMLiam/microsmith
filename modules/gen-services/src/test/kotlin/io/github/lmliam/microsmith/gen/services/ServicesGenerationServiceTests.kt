package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.services.core.ServiceBuilder
import io.github.lmliam.microsmith.dsl.services.core.ServicesExtension
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.Path

class ServicesGenerationServiceTests :
    StringSpec({
        "generate emits files for service extensions" {
            val emitter = TestServiceEmitter()
            val service =
                ServiceBuilder("UserService").apply {
                    put(TestServiceExtension::class, TestServiceExtension("hello"))
                }.build()
            val extension = ServicesExtension(setOf(service))
            val generationService = ServicesGenerationService(ServiceEmitterRegistry(listOf(emitter)))
            val space = DirectorySpace.from(Files.createTempDirectory("microsmith-services-gen-"))

            val generated = generationService.generate(extension, space)

            generated.size shouldBe 1
            generated.single().relativePath shouldBe Path("UserService.out")
            generated.single().contents.size shouldBe 0
        }
    })
