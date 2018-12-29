package com.lightningkite.kotlinx.db.influxdb

import com.lightningkite.kommon.native.SharedImmutable
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

@SharedImmutable
val TestRegistry = ClassInfoRegistry(
    com.lightningkite.kotlinx.db.influxdb.CPUUsageClassInfo,
    com.lightningkite.mirror.archive.model.HasIdClassInfo,
    com.lightningkite.lokalize.TimeStampClassInfo
)