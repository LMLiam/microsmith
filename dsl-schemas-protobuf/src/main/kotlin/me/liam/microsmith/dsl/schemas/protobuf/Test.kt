package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.microsmith
import me.liam.microsmith.dsl.schemas.core.schemas

fun test() {
    microsmith {
        schemas {
            protobuf {
                "me.liam.microsmith" {
                    "domainA" {
                        version(1) {
                            enum("Hello") {

                            }
                        }
                        version(2) {
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
}