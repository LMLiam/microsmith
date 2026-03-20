package io.github.lmliam.microsmith.build.maven

internal object MavenPluginBuildNames {
    const val GENERATE_GOAL = "generate"
    const val DESCRIPTOR_OUTPUT_PATH = "generated/resources/plugin/META-INF/maven/plugin.xml"
    const val RESOURCE_DIR = "generated/resources/plugin"
    const val PLUGIN_NAME = "Microsmith Maven Plugin"
    const val PLUGIN_DESCRIPTION = "Native Microsmith integration for Maven repositories"
    const val IMPLEMENTATION_CLASS = "io.github.lmliam.microsmith.maven.MicrosmithGenerateMojo"
    const val DESCRIPTOR_TASK_NAME = "generateMavenPluginDescriptor"
    const val VERIFY_TASK_NAME = "verifyMavenPluginDescriptor"
    const val PACKAGING = "maven-plugin"
}
