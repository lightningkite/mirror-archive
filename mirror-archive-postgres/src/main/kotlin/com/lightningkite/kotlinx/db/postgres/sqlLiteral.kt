package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.reflection.kxType


fun Any?.sqlLiteral(serializer: PostgresSerializer): String = if(this == null) "NULL" else serializer.write(this::class.kxType, this, Unit)
