package com.lightningkite.mirror.archive.postgres

data class Column(
        var name: String,
        var type: String,
        var size: Int? = null
) {
    fun toSql(): String = if (size == null) "$name $type" else "$name $type($size)"
    fun toLowerCase() {
        name = name.toLowerCase()
        type = type.toLowerCase()
    }
}