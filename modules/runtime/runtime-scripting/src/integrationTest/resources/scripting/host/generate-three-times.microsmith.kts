val model =
    microsmith {
        schemas {
            protobuf {
                "pkg" {
                    message("User") {
                        int32("id") { index(1) }
                    }
                }
            }
        }
    }

repeat(3) { generate(model) }
