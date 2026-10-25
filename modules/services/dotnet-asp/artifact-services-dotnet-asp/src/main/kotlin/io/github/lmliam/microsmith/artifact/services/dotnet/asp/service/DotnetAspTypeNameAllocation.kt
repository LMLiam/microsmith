package io.github.lmliam.microsmith.artifact.services.dotnet.asp.service

internal fun allocateDotnetAspTypeName(usedTypeNames: MutableSet<String>, vararg candidates: String): String {
    candidates
        .map(String::trim)
        .filter(String::isNotBlank)
        .firstOrNull { candidate -> usedTypeNames.add(candidate) }
        ?.let {
            return it
        }

    val fallbackBase = candidates.firstOrNull(String::isNotBlank)?.trim().orEmpty()
    var suffix = 2
    while (true) {
        val candidate = "$fallbackBase$suffix"
        if (usedTypeNames.add(candidate)) {
            return candidate
        }
        suffix += 1
    }
}
