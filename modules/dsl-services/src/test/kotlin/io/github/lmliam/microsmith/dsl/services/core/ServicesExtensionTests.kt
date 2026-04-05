package io.github.lmliam.microsmith.dsl.services.core

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ServicesExtensionTests :
    StringSpec({
        "find returns service when present" {
            val s1 = Service(name = "UserService", model = ServiceModel.empty())
            val ext = ServicesExtension(setOf(s1))

            ext.find("UserService") shouldBe s1
        }

        "find returns null when service not present" {
            val s1 = Service(name = "UserService", model = ServiceModel.empty())
            val ext = ServicesExtension(setOf(s1))

            ext.find("OrderService") shouldBe null
        }

        "require returns service when present" {
            val s1 = Service(name = "UserService", model = ServiceModel.empty())
            val ext = ServicesExtension(setOf(s1))

            ext.require("UserService") shouldBe s1
        }

        "require throws when service not present" {
            val s1 = Service(name = "UserService", model = ServiceModel.empty())
            val ext = ServicesExtension(setOf(s1))

            shouldThrow<IllegalStateException> {
                ext.require("OrderService")
            }
        }

        "find throws when service name is blank" {
            val ext = ServicesExtension(emptySet())

            shouldThrow<IllegalArgumentException> {
                ext.find(" ")
            }
        }

        "merge throws when duplicate service key exists across extensions" {
            val left = ServicesExtension(setOf(Service(name = "UserService", model = ServiceModel.empty())))
            val right = ServicesExtension(setOf(Service(name = "UserService", model = ServiceModel.empty())))

            shouldThrow<IllegalArgumentException> {
                left.merge(right)
            }
        }
    })
