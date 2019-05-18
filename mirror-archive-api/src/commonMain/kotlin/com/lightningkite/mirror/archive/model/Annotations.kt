package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass

@Target(AnnotationTarget.FIELD)
annotation class PrimaryKey()

fun <T : Any> MirrorClass<T>.findPrimaryKey(): List<MirrorClass.Field<T, *>> {
    return fields.filter {
        //Try for explicit annotation first
        it.annotations.any { it is PrimaryKeyMirror }
    }.takeUnless { it.isEmpty() } ?: fields.find {
        //Try for certain names next
        when (it.name) {
            "id",
            "identifier",
            "uuid" -> true
            else -> false
        }
    }?.let{ listOf(it) } ?: listOf(fields.first())
}

@Suppress("UNCHECKED_CAST")
fun <T : Any> List<MirrorClass.Field<T, *>>.sort(): List<Sort<T, *>> {
    return map {
        Sort(it as MirrorClass.Field<T, Comparable<Comparable<*>>>)
    }
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