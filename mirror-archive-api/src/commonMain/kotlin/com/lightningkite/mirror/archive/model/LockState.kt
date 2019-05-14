package com.lightningkite.mirror.archive.model

import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.lokalize.time.now
import kotlin.random.Random

inline class LockState(val value: Long) {

    val isLocked: Boolean get() = value == 0L

    companion object {
        val UNLOCKED = LockState(0L)
        fun get() = LockState(Random.nextLong() xor TimeStamp.now().millisecondsSinceEpoch)
    }
}