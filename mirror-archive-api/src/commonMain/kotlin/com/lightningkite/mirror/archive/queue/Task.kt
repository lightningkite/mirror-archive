package com.lightningkite.mirror.archive.queue

import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.mirror.archive.model.HasUuid

interface Task : HasUuid {
    val claimedBy: Int
    val claimedAt: TimeStamp
}