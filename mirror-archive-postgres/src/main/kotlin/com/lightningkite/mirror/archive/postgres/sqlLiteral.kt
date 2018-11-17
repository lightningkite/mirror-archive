package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.type


fun Any?.sqlLiteral(serializer: PostgresSerializer): String = if(this == null) "NULL" else buildString {
    @Suppress("UNCHECKED_CAST")
    serializer.encode<Any?>(this, this@sqlLiteral, this@sqlLiteral::class.type as Type<Any?>)
}
