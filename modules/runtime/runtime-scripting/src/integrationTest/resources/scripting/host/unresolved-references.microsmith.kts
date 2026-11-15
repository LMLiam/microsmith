microsmith {
    schemas {
        protobuf {
            "pkg" {
                message("User") {
                    ref("owner", "MissingOwner") { index(1) }
                    ref("group", "MissingGroup") { index(2) }
                }
            }
        }
    }
}
