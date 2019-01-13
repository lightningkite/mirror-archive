package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.info.FieldInfo

class Relation<OK, OV : Any, MK, MV : Any>(
        val manyMap: SuspendMap<MK, MV>,
        val field: FieldInfo<MV, Reference<OK, OV>>
) {
    suspend fun get(key: OK, after: SuspendMap.Entry<MK, MV>? = null, count: Int = 100): List<SuspendMap.Entry<MK, MV>> {
        return manyMap.query(
                condition = Condition.Field(field, Condition.Equal(Reference(key))),
                after = after,
                count = count
        )
    }
}