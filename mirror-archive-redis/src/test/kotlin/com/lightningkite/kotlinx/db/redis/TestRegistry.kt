package com.lightningkite.kotlinx.db.redis

import com.lightningkite.kommon.native.SharedImmutable
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

@SharedImmutable
val TestRegistry = ClassInfoRegistry(
        com.lightningkite.mirror.archive.model.IdClassInfo
)
