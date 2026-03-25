package io.github.lmliam.microsmith.gen.services

import io.github.lmliam.microsmith.dsl.services.core.ServiceBuilder
import io.github.lmliam.microsmith.gen.files.DirectorySpace
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Files
import kotlin.io.path.Path

class ServicesGenerationServiceTests :
    StringSpec({
        "generate emits files for shared and service extensions in declaration order" {
            val sharedEmitter = TestSharedEmitter()
            val serviceEmitter = TestServiceEmitter()
            val service =
                ServiceBuilder("UserService").apply {
                    put(TestServiceExtension::class, TestServiceExtension("hello"))
                }.build()
            val builder =
                io.github.lmliam.microsmith.dsl.services.core.ServicesBuilder().apply {
                    put(TestSharedExtension::class, TestSharedExtension("root"))
                    register(service)
                }
            val extension = builder.toExtension()
            val generationService =
                ServicesGenerationService(
                    sharedEmitterRegistry = SharedServiceEmitterRegistry(listOf(sharedEmitter)),
                    emitterRegistry = ServiceEmitterRegistry(listOf(serviceEmitter)),
                )
            val space = DirectorySpace.from(Files.createTempDirectory("microsmith-services-gen-"))

            val generated = generationService.generate(extension, space)

            generated.size shouldBe 2
            generated[0].relativePath shouldBe Path("shared.out")
            generated[1].relativePath shouldBe Path("UserService.out")
            generated.map { it.contents.size } shouldBe listOf(0, 0)
        }
    })
