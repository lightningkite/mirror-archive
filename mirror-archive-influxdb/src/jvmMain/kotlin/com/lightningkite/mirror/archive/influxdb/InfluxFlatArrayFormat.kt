package com.lightningkite.mirror.archive.influxdb

import com.lightningkite.lokalize.time.TimeStampMirror
import com.lightningkite.mirror.archive.flatarray.FlatArrayFormat
import com.lightningkite.mirror.archive.flatarray.StringFlatArrayFormat
import com.lightningkite.mirror.archive.model.UuidMirror

object InfluxFlatArrayFormat : StringFlatArrayFormat(
        terminateAt = {
            when (it) {
                TimeStampMirror,
                UuidMirror -> true
                else -> false
            }
        }
)
