package com.lightningkite.mirror.archive.model

@Suppress("unused") /*inline*/ data class Reference<MODEL : HasId>(val id: Id)

fun <MODEL : HasId> MODEL.key(): Reference<MODEL>? = Reference(id)