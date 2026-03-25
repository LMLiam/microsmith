package io.github.lmliam.microsmith.gen.services

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class SharedServiceEmitterRegistryTests :
    StringSpec({
        "resolve returns emitter for matching shared service extension class" {
            val emitter = TestSharedEmitter()
            val registry = SharedServiceEmitterRegistry(listOf(emitter))

            registry.resolve(TestSharedExtension()).type shouldBe TestSharedExtension::class
        }

        "resolve rejects duplicate emitters for the same shared service extension class" {
            shouldThrow<IllegalArgumentException> {
                SharedServiceEmitterRegistry(listOf(TestSharedEmitter(), DuplicateTestSharedEmitter()))
            }
        }

        "resolve rejects missing emitter" {
            val registry = SharedServiceEmitterRegistry(emptyList())

            shouldThrow<IllegalStateException> {
                registry.resolve(TestSharedExtension())
            }
        }
    })
