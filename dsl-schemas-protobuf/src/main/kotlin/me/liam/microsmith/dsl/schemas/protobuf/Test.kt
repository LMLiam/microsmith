package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.microsmith
import me.liam.microsmith.dsl.helpers.extensions
import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.dsl.schemas.core.schemas

fun main() {
    val model = microsmith {
        schemas {
            protobuf {
                "me.liam" {
                    1 {
                        enum("wanker") {
                            +"shallow"
                        }

                        message("hello") {
                            string("hello") {
                                index(500)
                                optional()
                            }
                            ref("hello2", "wanker")
                            map("wanker") {
                                types(string to ref("wanker"))
                            }
                        }
                    }
                }
            }
        }
    }

    model.extensions().filterIsInstance<SchemasExtension>()
        .flatMap { it.schemas }
        .filterIsInstance<ProtobufSchema>()
        .forEach { println(it) }

}