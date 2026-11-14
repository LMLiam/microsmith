package io.github.lmliam.microsmith.gen

import java.net.URL
import java.nio.file.Files
import java.util.Collections
import java.util.Enumeration
import kotlin.reflect.KClass

class ServiceProviderClassLoader(
    parent: ClassLoader,
    private val registrations: Map<KClass<*>, List<KClass<*>>>,
) : ClassLoader(parent) {
    override fun getResources(name: String): Enumeration<URL> {
        val registration =
            registrations.entries.firstOrNull { (contract, _) -> name == "META-INF/services/${contract.java.name}" }
                ?: return super.getResources(name)

        val file = Files.createTempFile("microsmith-service-registration-", ".txt")
        Files.write(file, registration.value.map { provider -> provider.java.name })
        file.toFile().deleteOnExit()

        return Collections.enumeration(listOf(file.toUri().toURL()))
    }
}
