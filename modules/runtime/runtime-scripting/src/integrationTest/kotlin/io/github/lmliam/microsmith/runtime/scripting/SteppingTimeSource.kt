package io.github.lmliam.microsmith.runtime.scripting

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class SteppingTimeSource(private val step: Duration) : TimeSource {
    private var current = Duration.ZERO

    override fun markNow(): TimeMark {
        val start = tick()
        return object : TimeMark {
            override fun elapsedNow(): Duration = tick() - start
        }
    }

    private fun tick(): Duration {
        current += step
        return current
    }
}
