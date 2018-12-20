package com.lightningkite.kotlinx.db.influxdb

import com.lightningkite.lokalize.TimeStamp
import com.lightningkite.lokalize.now
import com.lightningkite.mirror.archive.HasId
import com.lightningkite.mirror.archive.Id

data class CPUUsage(
        override val id: Id = Id.key(),
        val time: TimeStamp = TimeStamp.now(),
        val amount: Double = 0.0
) : HasId {
}