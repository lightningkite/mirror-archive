package com.lightningkite.mirror.archive

data class ChangeEvent<T: HasId>(val item: T, val type: ChangeEvent.Type) {

    enum class Type {
        Insertion, Modification, Deletion
    }
}
