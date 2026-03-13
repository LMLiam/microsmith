microsmith {
    schemas {
        protobuf {
            message("JavaGradleUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
