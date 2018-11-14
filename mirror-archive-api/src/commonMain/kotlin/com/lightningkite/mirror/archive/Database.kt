package com.lightningkite.mirror.archive

import com.lightningkite.kotlinx.reflection.KxClass

interface Database {
    fun <T: Model<ID>, ID> table(type: KxClass<T>, name: String = type.simpleName): DatabaseTable<T, ID>
}