package me.liam.microsmith

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.junitxml.JunitXmlReporter

class KotestConfig : AbstractProjectConfig() {
    override val extensions = listOf(
        JunitXmlReporter(
            includeContainers = false,
            useTestPathAsName = true,
            outputDir = "${System.getProperty("gradle.build.dir")}/test-results/kotest"
        )
    )
}