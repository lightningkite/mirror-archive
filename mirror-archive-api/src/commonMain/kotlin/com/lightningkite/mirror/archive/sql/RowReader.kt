package com.lightningkite.mirror.archive.sql

import com.lightningkite.mirror.info.Type

interface RowReader {
    fun peekIsNull(): Boolean
    fun <T> readColumnAndAdvance(type: Type<T>): T?
    fun skipColumn()
    fun nextRow(): Boolean
}