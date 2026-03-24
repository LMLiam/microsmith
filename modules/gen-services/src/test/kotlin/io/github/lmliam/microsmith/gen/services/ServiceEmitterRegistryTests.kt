package io.github.lmliam.microsmith.gen.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ServiceEmitterRegistryTests :
    StringSpec({
        "resolve returns emitter for matching service extension class" {
            val emitter = TestServiceEmitter()
            val registry = ServiceEmitterRegistry(listOf(emitter))

            registry.resolve(TestServiceExtension()).type shouldBe TestServiceExtension::class
        }

        "resolve rejects duplicate emitters for the same service extension class" {
            shouldThrow<IllegalArgumentException> {
                ServiceEmitterRegistry(listOf(TestServiceEmitter(), DuplicateTestServiceEmitter()))
            }
        }

        "resolve rejects missing emitter" {
            val registry = ServiceEmitterRegistry(emptyList())

            shouldThrow<IllegalStateException> {
                registry.resolve(TestServiceExtension())
            }
        }
    })
