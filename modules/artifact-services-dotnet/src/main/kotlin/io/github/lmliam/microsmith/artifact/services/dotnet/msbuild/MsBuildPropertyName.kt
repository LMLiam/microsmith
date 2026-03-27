package io.github.lmliam.microsmith.artifact.services.dotnet.msbuild

@JvmInline
value class MsBuildPropertyName(
    val value: String,
) {
    init {
        require(value.isNotBlank()) { "MSBuild property name cannot be blank." }
        require(value.none(Char::isWhitespace)) { "MSBuild property name cannot contain whitespace: '$value'." }
    }

    override fun toString(): String = value

    companion object {
        val ManagePackageVersionsCentrally = MsBuildPropertyName("ManagePackageVersionsCentrally")

        fun of(value: String): MsBuildPropertyName = MsBuildPropertyName(value)
    }
}
