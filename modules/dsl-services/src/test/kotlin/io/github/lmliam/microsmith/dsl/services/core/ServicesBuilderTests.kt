package io.github.lmliam.microsmith.dsl.services.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly

class ServicesBuilderTests :
    StringSpec({
        "register adds service to builder" {
            val builder = ServicesBuilder()
            val service = Service(name = "UserService", model = ServiceModel.empty())

            builder.register(service)

            builder.services shouldContainExactly listOf(service)
        }

        "register throws if service name is blank" {
            val builder = ServicesBuilder()

            shouldThrow<IllegalArgumentException> {
                builder.register(Service(name = " ", model = ServiceModel.empty()))
            }
        }

        "register throws for duplicate service name" {
            val builder = ServicesBuilder()
            val service = Service(name = "UserService", model = ServiceModel.empty())

            builder.register(service)

            shouldThrow<IllegalArgumentException> {
                builder.register(Service(name = "UserService", model = ServiceModel.empty()))
            }
        }

        "build produces ServicesExtension with all services" {
            val builder = ServicesBuilder()
            val s1 = Service(name = "UserService", model = ServiceModel.empty())
            val s2 = Service(name = "OrderService", model = ServiceModel.empty())

            builder.register(s1)
            builder.register(s2)

            val ext = builder.toExtension()

            ext.services shouldContainExactly listOf(s1, s2)
        }

        "ServicesExtension is immutable snapshot" {
            val builder = ServicesBuilder()
            val service = Service(name = "UserService", model = ServiceModel.empty())
            builder.register(service)

            val ext = builder.toExtension()

            builder.register(Service(name = "OrderService", model = ServiceModel.empty()))

            ext.services shouldContainExactly listOf(service)
        }
    })
