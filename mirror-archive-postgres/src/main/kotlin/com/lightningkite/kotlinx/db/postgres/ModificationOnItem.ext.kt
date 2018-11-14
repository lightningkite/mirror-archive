package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.persistence.ModificationOnItem

fun <T: Any> ModificationOnItem<T, *>.sql(serializer: PostgresSerializer) = when(this) {
    is ModificationOnItem.Set<*, *> -> "${field.name} = ${value.sqlLiteral(serializer)}"
    is ModificationOnItem.Add<*, *> -> "${field.name} = ${field.name} + ${amount.sqlLiteral(serializer)}"
    is ModificationOnItem.Multiply<*, *> -> "${field.name} = ${field.name} * ${amount.sqlLiteral(serializer)}"
}
