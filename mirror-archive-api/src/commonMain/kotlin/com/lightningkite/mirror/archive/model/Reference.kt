package com.lightningkite.mirror.archive.model

@Suppress("unused") /*inline*/ data class Reference<KEY, VALUE>(val key: KEY)

fun <MODEL : HasId> MODEL.key(): Reference<Id, MODEL>? = Reference(id)