package com.lightningkite.mirror.archive.sql

data class Column(
        var name: String,
        var type: String,
        var size: Int? = null
) {
    init{
        name = name.toLowerCase()
        type = type.toLowerCase()
    }
    fun noNameToValue(): Column = if(name.isBlank()) copy(name = "value") else this
}