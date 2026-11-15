val entityName = requireVar("entityName")
if (!hasFlag("emit")) {
    error("Expected emit flag")
}

emit(
    microsmith {
        schemas {
            protobuf {
                message(entityName) {
                    int32("id") { index(1) }
                }
            }
        }
    },
)
