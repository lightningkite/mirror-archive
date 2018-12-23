package com.lightningkite.mirror.archive.sql

import com.lightningkite.mirror.info.Type

interface RowWriter {
    fun <T> writeColumnAndAdvance(type: Type<T>, value:T)
}