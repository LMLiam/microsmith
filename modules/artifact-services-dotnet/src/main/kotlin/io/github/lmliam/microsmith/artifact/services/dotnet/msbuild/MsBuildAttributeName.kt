package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

@JvmInline
value class MsBuildAttributeName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "MSBuild attribute name cannot be blank." }
        require(value.none(Char::isWhitespace)) { "MSBuild attribute name cannot contain whitespace: '$value'." }
    }

    override fun toString(): String = value

    companion object {
        val Version = MsBuildAttributeName("Version")

        fun of(value: String): MsBuildAttributeName = MsBuildAttributeName(value)
    }
}
