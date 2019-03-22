package com.lightningkite.mirror.archive.postgres

import io.reactiverse.pgclient.Tuple
import io.vertx.core.buffer.Buffer
import java.lang.Appendable
import java.lang.IllegalArgumentException


class QueryBuilder(val builder: StringBuilder = StringBuilder(), val arguments: Tuple = Tuple.tuple()) : Appendable by builder {
    fun appendValue(it: Any?) {
        when (it) {
            null -> arguments.addValue(null)
            is Unit -> arguments.addShort(0)
            is Boolean -> arguments.addBoolean(it)
            is Byte -> arguments.addShort(it.toShort())
            is Short -> arguments.addShort(it)
            is Int -> arguments.addInteger(it)
            is Long -> arguments.addLong(it)
            is Float -> arguments.addFloat(it)
            is Double -> arguments.addDouble(it)
            is Char -> arguments.addString(it.toString())
            is String -> arguments.addString(it)
            is ByteArray -> arguments.addBuffer(Buffer.buffer(it))
            else -> throw IllegalArgumentException()
        }
        builder.append('$')
        builder.append(arguments.size())
    }

    fun appendFieldName(name: String) {
        if (name.isEmpty()) append("value")
        else append(name)
    }
}