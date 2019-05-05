package com.lightningkite.mirror.archive.influxdb

import java.lang.Appendable
import java.lang.IllegalArgumentException


class QueryBuilder(val builder: StringBuilder = StringBuilder()) : Appendable by builder {
    fun <T> appendValue(it: T?) {
        val str = when (it) {
            null -> "null"
            is Unit -> "0"
//            is ByteArray -> "'" + it.toS.replace("'", "\\'") + "'" //TODO: Base64
            is Boolean,
            is Byte,
            is Short,
            is Int,
            is Long,
            is Float,
            is Double -> it.toString()
            is Char -> "'" + it.toString().replace("'", "\\'") + "'"
            is String -> "'" + it.replace("'", "\\'") + "'"
            else -> throw IllegalArgumentException()
//            else -> "'" + backupStringSerializer.write(this, type as Type<Any?>).replace("'", "\\'") + "'"
        }
        append(str)
    }

    fun appendFieldName(name: String) {
        if (name.isEmpty()) append("value")
        else {
            append('\"')
            append(name)
            append('\"')
        }
    }

    override fun toString(): String {
        println("Query is: ${builder.toString()}")
        return builder.toString()
    }
}