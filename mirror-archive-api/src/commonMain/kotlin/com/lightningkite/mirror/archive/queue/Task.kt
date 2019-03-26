package com.lightningkite.mirror.archive.queue

import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.mirror.archive.model.HasId

interface Task : HasId {
    val claimedBy: Int
    val claimedAt: TimeStamp
}