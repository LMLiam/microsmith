package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.microsmith
import me.liam.microsmith.dsl.helpers.extensions
import me.liam.microsmith.dsl.schemas.core.SchemasExtension
import me.liam.microsmith.dsl.schemas.core.schemas

fun main() {
    val model = microsmith {
        schemas {
            protobuf {
                "me.liam.microsmith" {
                    "domainA" {
                        version(1) {
                            message("RefTest") {
                                ref("somehow", ".v2.Empty") {
                                    index(100)
                                }
                                ref("another", "..domainB.something")
                            }

                            enum("Hello") {
                                +"World"
                                +"Wanker"
                                reserved {
                                    +(50..max)
                                    +"Fuck"
                                }
                            }
                        }
                        version(2) {
                            message("Empty")

                            message("User") {
                                string("name")
                                string("email_address") {
                                    index(1000)
                                }
                                oneof("contact_info") {
                                    string("phone_number")
                                    string("telegram_handle")
                                }
                            }

                            enum("Hello") {

                            }
                        }
                    }

                    "domainB" {
                        message("something") {

                        }
                    }
                }

                enum("Hello") {
                    value("World") {
                        index(5)
                    }
                    reserved {
                        +(1..4)
                    }
                }
            }
        }
    }
    val schemas = model.extensions().find { it is SchemasExtension }
    println(schemas)
}