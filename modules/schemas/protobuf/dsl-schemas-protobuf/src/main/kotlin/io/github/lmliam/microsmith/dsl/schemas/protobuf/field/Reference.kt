package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.ProtobufTypeRef

sealed interface Reference : ValueType {
    data class Local(val target: String) : Reference

    data class Relative(val levelsUp: Int, val path: List<String>) : Reference {
        init {
            require(levelsUp > 0) { "Relative reference must go up at least one level: $levelsUp" }
            require(path.isNotEmpty() && path.none(String::isBlank)) {
                "Relative reference path contains empty segments: $path"
            }
        }
    }

    data class Qualified(val qualifiedName: String) : Reference

    data class Symbolic(val target: ProtobufTypeRef) : Reference

    fun resolveSegments(scope: List<String>): List<String> =
        when (this) {
            is Local -> scope + target
            is Relative -> scope.dropLast(levelsUp.coerceAtMost(scope.size)) + path
            is Qualified -> qualifiedName.split('.')
            is Symbolic -> target.qualifiedName.split('.')
        }

    companion object {
        fun parse(target: String): Reference {
            require(target.isNotBlank()) { "Reference target cannot be blank." }

            val levelsUp = target.takeWhile { it == '.' }.length
            val remaining = target.drop(levelsUp)
            require(remaining.isNotBlank()) { "Reference target cannot end with only dots: '$target'" }

            val path = remaining.split('.')
            require(path.none(String::isBlank)) { "Reference target contains empty path segments: '$target'" }

            return when {
                levelsUp > 0 -> Relative(levelsUp, path)
                path.size > 1 -> Qualified(target)
                else -> Local(target)
            }
        }
    }
}
