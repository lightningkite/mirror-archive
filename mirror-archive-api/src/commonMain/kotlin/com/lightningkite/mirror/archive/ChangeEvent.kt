package com.lightningkite.mirror.archive

data class ChangeEvent<T: Model<ID>, ID>(val item: T, val type: ChangeEvent.Type) {

    @ExternalReflection
    enum class Type {
        Insertion, Modification, Deletion
    }
}
