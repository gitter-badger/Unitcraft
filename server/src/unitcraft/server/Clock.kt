package unitcraft.server

import java.time.Duration
import java.time.Instant

class Clock(start:Duration,val extend:Duration) {
    var left = start
        private set
    private var last: Instant? = null

    fun start(now: Instant) {
        if (!stoped()) throw Err("clock already started")
        if (elapsed()) throw Err("clock already elapsed")
        last = now
    }

    fun stop() {
        last = null
    }

    fun extend() {
        left += extend
    }

    fun elapsed() = left.isZero

    fun stoped() = last == null

    fun update(now: Instant) {
        if (!stoped()) {
            val dur = left.minus(Duration.between(last, now))!!
            left = if (dur.isNegative) Duration.ZERO else dur
            last = if (left.isZero) null else now
        }
    }
}