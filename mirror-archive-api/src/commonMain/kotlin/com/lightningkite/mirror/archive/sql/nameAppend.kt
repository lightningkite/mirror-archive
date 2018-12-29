package com.lightningkite.mirror.archive.sql



infix fun String.nameAppend(other: String): String = when {
    this.isBlank() && other.isBlank() -> "value"
    this.isBlank() -> other
    other.isBlank() -> this
    else -> this + "_" + other
}