package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.serialization.externalName

interface Database {
    fun <T: Model<ID>, ID> table(type: ClassInfo<T>, name: String = type.kClass.externalName): DatabaseTable<T, ID>
}