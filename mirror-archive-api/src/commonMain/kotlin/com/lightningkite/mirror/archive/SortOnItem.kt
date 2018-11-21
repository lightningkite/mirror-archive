package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.FieldInfo

data class SortOnItem<T : Any, V: Comparable<V>>(
        val field: FieldInfo<T, V>,
        val ascending: Boolean = true,
        val nullsFirst: Boolean = false
)
