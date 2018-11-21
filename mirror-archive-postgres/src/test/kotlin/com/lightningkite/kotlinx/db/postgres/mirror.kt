package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kommon.native.SharedImmutable
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

@SharedImmutable
val TestRegistry = ClassInfoRegistry(
    com.lightningkite.kotlinx.db.postgres.PostClassInfo
)