package me.liam.microsmith.build.quality

import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RepositoryQualityPluginTests {
    @Test
    fun `apply registers verify task and wires root check`() {
        val project = ProjectBuilder.builder().withName("root").build()

        project.pluginManager.apply("me.liam.microsmith.repository-quality")

        val verifyTask = project.tasks.findByName("verifyRepositoryStandards")
        val checkTask = project.tasks.findByName("check")

        assertNotNull(verifyTask)
        assertNotNull(checkTask)
        assertTrue(checkTask.taskDependencies.getDependencies(checkTask).contains(verifyTask))
    }

    @Test
    fun `apply rejects non-root projects`() {
        val rootProject = ProjectBuilder.builder().withName("root").build()
        val childProject = ProjectBuilder.builder().withName("child").withParent(rootProject).build()

        val failure = assertFailsWith<PluginApplicationException> { childProject.pluginManager.apply("me.liam.microsmith.repository-quality") }
        val rootOnlyFailure = assertIs<IllegalArgumentException>(failure.cause)

        assertTrue(
            actual = rootOnlyFailure.message.orEmpty().contains("root project"),
            message = "Expected root-only guidance in the failure message.",
        )
    }
}
