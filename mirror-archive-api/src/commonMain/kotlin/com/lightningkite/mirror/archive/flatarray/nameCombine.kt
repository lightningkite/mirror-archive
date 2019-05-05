package com.lightningkite.mirror.archive.flatarray


infix fun String.nameCombine(other: String): String {
    return if (this.isBlank()) other
    else if (other.isBlank()) this
    else this + "_" + other
}