package com.lightningkite.mirror.archive.model

data class ChangeEvent<T>(val item: T, val type: ChangeEvent.Type) {

    enum class Type {
        Insertion, Modification, Deletion
    }
}
