package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.SerializedFieldInfo

data class SortOnItem<T : Any, V: Comparable<V>>(
        val field: SerializedFieldInfo<T, V>,
        val ascending: Boolean = true,
        val nullsFirst: Boolean = false
)
