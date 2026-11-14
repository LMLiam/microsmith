package io.github.lmliam.microsmith.build.layers

@JvmInline
internal value class JvmPackage(
    val internalName: String,
)

@JvmInline
internal value class JvmClassName(
    val internalName: String,
) : Comparable<JvmClassName> {
    private val packageName: String get() = internalName.substringBeforeLast('/', "")

    fun isIn(jvmPackage: JvmPackage): Boolean =
        packageName == jvmPackage.internalName || packageName.startsWith("${jvmPackage.internalName}/")

    override fun compareTo(other: JvmClassName): Int = internalName.compareTo(other.internalName)
}

private const val SUPERTYPES_SEPARATOR = " : "
private const val HEADER_OPENER = " {"
private const val OBJECT_TYPE_START = 'L'
private const val OBJECT_TYPE_END = ';'

internal fun referencedClasses(dumpLines: List<String>): Set<JvmClassName> =
    dumpLines
        .flatMap { line ->
            when {
                line.isBlank() || line.trim() == "}" -> emptyList()
                line.startsWith('\t') -> descriptorClasses(line.trim().substringAfterLast(' '))
                else -> supertypes(line)
            }
        }.toSortedSet()

private fun supertypes(header: String): List<JvmClassName> =
    header
        .substringAfter(SUPERTYPES_SEPARATOR, "")
        .removeSuffix(HEADER_OPENER)
        .split(", ")
        .filter { it.isNotBlank() }
        .map(::JvmClassName)

private fun descriptorClasses(descriptor: String): List<JvmClassName> =
    buildList {
        var index = 0
        while (index < descriptor.length) {
            if (descriptor[index] == OBJECT_TYPE_START) {
                val end = descriptor.indexOf(OBJECT_TYPE_END, index)
                require(end > index) { "Unterminated object type in descriptor $descriptor" }
                add(JvmClassName(descriptor.substring(index + 1, end)))
                index = end + 1
            } else {
                index++
            }
        }
    }
