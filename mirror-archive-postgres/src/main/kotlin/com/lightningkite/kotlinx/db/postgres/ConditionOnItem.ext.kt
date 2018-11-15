package com.lightningkite.kotlinx.db.postgres

fun <T: Any> ConditionOnItem<T>.sql(serializer: PostgresSerializer): String = when(this){
    is ConditionOnItem.Never -> "FALSE"
    is ConditionOnItem.Always -> "TRUE"
    is ConditionOnItem.And -> this.conditions.joinToString(" AND ", "(", ")"){ it.sql(serializer) }
    is ConditionOnItem.Or -> this.conditions.joinToString(" OR ", "(", ")"){ it.sql(serializer) }
    is ConditionOnItem.Not -> "NOT (${condition.sql(serializer)})"
    is ConditionOnItem.Equal<*, *> -> "${field.name} = ${value.sqlLiteral(serializer)}"
    is ConditionOnItem.EqualToOne<*, *> -> "${field.name} IN (${values.joinToString(", "){it.sqlLiteral(serializer)}}"
    is ConditionOnItem.NotEqual<*, *> -> "${field.name} <> ${value.sqlLiteral(serializer)}"
    is ConditionOnItem.LessThan<*, *> -> "${field.name} < ${value.sqlLiteral(serializer)}"
    is ConditionOnItem.GreaterThan<*, *> -> "${field.name} > ${value.sqlLiteral(serializer)}"
    is ConditionOnItem.LessThanOrEqual<*, *> -> "${field.name} <= ${value.sqlLiteral(serializer)}"
    is ConditionOnItem.GreaterThanOrEqual<*, *> -> "${field.name} >= ${value.sqlLiteral(serializer)}"
    is ConditionOnItem.TextSearch<*, *> -> "${field.name} LIKE ${("%$query%").sqlLiteral(serializer)}"
    is ConditionOnItem.RegexTextSearch<*, *> -> "${field.name} SIMILAR TO ${query.sqlLiteral(serializer)}"
}
