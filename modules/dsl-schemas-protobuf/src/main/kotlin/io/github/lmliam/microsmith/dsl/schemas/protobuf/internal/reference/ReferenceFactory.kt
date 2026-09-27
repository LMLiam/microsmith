package io.github.lmliam.microsmith.dsl.schemas.protobuf.internal.reference

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference

internal fun textualReference(target: String): Reference {
    require(target.isNotBlank()) { "Reference target cannot be blank." }

    if (target.startsWith(".")) {
        val dotCount = target.takeWhile { it == '.' }.length
        val remaining = target.drop(dotCount)
        require(remaining.isNotBlank()) { "Reference target cannot end with only dots: '$target'" }

        require(remaining.split('.').none(String::isBlank)) {
            "Reference target contains empty path segments: '$target'"
        }

        return Reference.Relative(target)
    }

    if ('.' in target) {
        require(target.split('.').none(String::isBlank)) { "Reference target contains empty path segments: '$target'" }
        return Reference.Qualified(target)
    }

    return Reference.Local(target)
}
