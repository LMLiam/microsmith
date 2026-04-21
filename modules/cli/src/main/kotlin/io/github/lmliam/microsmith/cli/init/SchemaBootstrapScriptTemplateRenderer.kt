package io.github.lmliam.microsmith.cli.init

internal object SchemaBootstrapScriptTemplateRenderer {
    fun render(profile: OnboardingProfile): String = """
        microsmith {
            schemas {
                protobuf {
                    message("${profile.sampleMessageName}") {
                        int32("id") { index(1) }
                        string("email") { index(2) }
                    }
                }
            }
        }
    """.trimIndent()
}
