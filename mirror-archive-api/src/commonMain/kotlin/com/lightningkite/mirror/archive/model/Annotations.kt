package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass

@Target(AnnotationTarget.FIELD)
annotation class PrimaryKey()

fun <T : Any> MirrorClass<T>.findPrimaryKey(): MirrorClass.Field<T, *> {
    return fields.find {
        //Try for explicit annotation first
        it.annotations.any { it is PrimaryKeyMirror }
    } ?: fields.find {
        //Try for certain names next
        when (it.name) {
            "id",
            "identifier",
            "uuid" -> true
            else -> false
        }
    } ?: fields.first()
}


@Target(AnnotationTarget.FIELD)
annotation class Indexed()

val MirrorClass.Field<*, *>.shouldBeIndexed: Boolean
    get() {
        return annotations.any { it is IndexedMirror }
    }


@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class MultiIndex(val fields: Array<String>)

fun <T : Any> MirrorClass<T>.multiIndexSequence(): Sequence<List<MirrorClass.Field<T, *>>> {
    return annotations.asSequence().mapNotNull { it as? MultiIndexMirror }.map {
        it.fields.map { fieldName -> this.fields.find { it.name == fieldName }!! }
    }
}