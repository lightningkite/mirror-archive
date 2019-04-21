package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.HasId
import com.lightningkite.mirror.archive.model.Uuid
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.info.type

suspend fun <T: Any, F> Database<T>.get(field: MirrorClass.Field<T, F>, value: F): T? = get(
        condition = Condition.Field(field, Condition.Equal(value)),
        count = 1
).firstOrNull()

@Suppress("UNCHECKED_CAST")
suspend inline fun <reified T: HasId> Database<T>.get(id: Uuid) = get(T::class.type.fields.find { it.name == "id" }!! as MirrorClass.Field<T, Uuid>, id)