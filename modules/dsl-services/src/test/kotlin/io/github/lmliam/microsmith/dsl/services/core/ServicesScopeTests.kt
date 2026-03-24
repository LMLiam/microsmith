package io.github.lmliam.microsmith.dsl.services.core

import io.github.lmliam.microsmith.dsl.core.MicrosmithBuilder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private fun ServicesScope.fake(name: String, block: ServiceScope.() -> Unit = {}) {
    name(block)
}

class ServicesScopeTests :
    StringSpec({
        "services block attaches ServicesExtension to builder" {
            val builder = MicrosmithBuilder()

            builder.services {
                fake("UserService")
            }

            val ext = builder.model.get<ServicesExtension>()
            ext shouldBe ServicesExtension(setOf(Service(name = "UserService", model = ServiceModel.empty())))
        }

        "services block can register multiple services" {
            val builder = MicrosmithBuilder()

            builder.services {
                fake("UserService")
                fake("OrderService")
            }

            val ext = builder.model.get<ServicesExtension>()
            ext shouldBe
                ServicesExtension(
                    setOf(
                        Service(name = "UserService", model = ServiceModel.empty()),
                        Service(name = "OrderService", model = ServiceModel.empty()),
                    ),
                )
        }

        "multiple services blocks are merged" {
            val builder = MicrosmithBuilder()

            builder.services {
                fake("UserService")
            }

            builder.services {
                fake("OrderService")
            }

            val ext = builder.model.get<ServicesExtension>()
            ext shouldBe
                ServicesExtension(
                    setOf(
                        Service(name = "UserService", model = ServiceModel.empty()),
                        Service(name = "OrderService", model = ServiceModel.empty()),
                    ),
                )
        }

        "multiple services blocks reject duplicate service keys" {
            val builder = MicrosmithBuilder()

            builder.services {
                fake("UserService")
            }

            shouldThrow<IllegalArgumentException> {
                builder.services {
                    fake("UserService")
                }
            }
        }
    })
