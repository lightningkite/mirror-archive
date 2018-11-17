package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.SerializedFieldInfo
import java.util.*

/**
 * Returns the primary key of the type.
 */
fun <T : Any> ClassInfo<T>.primaryKey(): SerializedFieldInfo<T, *> = fields.find { it.name == "id" }!!