package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import arrow.core.getOrElse
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan

internal fun singleSpan(number: Int, domain: ReservedSpan.Domain = ReservedSpan.Domain.MESSAGE): ReservedSpan =
    ReservedSpan.single(number, domain).getOrElse { error("$number is not a reservable number") }

internal fun rangeSpan(
    first: Int,
    last: Int,
    domain: ReservedSpan.Domain = ReservedSpan.Domain.MESSAGE,
): ReservedSpan = ReservedSpan.range(first, last, domain).getOrElse { error("$first..$last is not a reservable range") }

internal fun toMaxSpan(first: Int, domain: ReservedSpan.Domain = ReservedSpan.Domain.MESSAGE): ReservedSpan =
    ReservedSpan.toMax(first, domain).getOrElse { error("$first is not a reservable start") }
