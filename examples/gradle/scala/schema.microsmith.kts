microsmith {
    schemas {
        protobuf {
            message("ScalaGradleUserCreated") {
                int32("id") { index(1) }
                string("email") { index(2) }
            }
        }
    }
}
