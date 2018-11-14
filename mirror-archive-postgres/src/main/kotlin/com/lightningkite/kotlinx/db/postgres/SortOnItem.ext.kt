package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.persistence.SortOnItem

fun <T: Any> SortOnItem<T, *>.sql(): String{
    val nulls = if(nullsFirst) "NULLS FIRST" else "NULLS LAST"
    val asc = if(ascending) "ASC" else "DESC"
    return "${field.name} $asc $nulls"
}