package io.github.lmliam.microsmith.gen.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ServiceEmitterRegistryTests :
    StringSpec({
        "resolve returns emitters for matching extension class" {
            val emitter = TestServiceEmitter()
            val registry = ServiceEmitterRegistry(listOf(emitter))

            registry.resolve(TestServiceExtension()).map(ServiceEmitter<*>::type) shouldBe
                listOf(TestServiceExtension::class)
        }

        "resolve also handles services-level extensions" {
            val registry = ServiceEmitterRegistry(listOf(TestSharedEmitter()))

            registry.resolve(TestSharedExtension()).map(ServiceEmitter<*>::type) shouldBe
                listOf(TestSharedExtension::class)
        }

        "resolve allows multiple emitters for the same extension class" {
            val registry = ServiceEmitterRegistry(listOf(TestServiceEmitter(), AdditionalTestServiceEmitter()))

            registry.resolve(TestServiceExtension()).map { it::class } shouldBe
                listOf(AdditionalTestServiceEmitter::class, TestServiceEmitter::class)
        }

        "resolve rejects duplicate emitter implementations for the same extension class" {
            shouldThrow<IllegalArgumentException> {
                ServiceEmitterRegistry(listOf(TestServiceEmitter(), TestServiceEmitter()))
            }
        }

        "resolve rejects missing emitter" {
            val registry = ServiceEmitterRegistry(emptyList())

            shouldThrow<IllegalStateException> {
                registry.resolve(TestServiceExtension())
            }
        }
    })
