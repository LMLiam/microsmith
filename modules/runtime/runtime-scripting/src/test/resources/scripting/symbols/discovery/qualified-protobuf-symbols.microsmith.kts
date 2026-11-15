microsmith {
    schemas {
        protobuf {
            "acme.user" {
                version(2) {
                    message("User")
                    enum("Role")
                }
            }
        }
    }
}
