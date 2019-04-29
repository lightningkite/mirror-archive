package com.lightningkite.mirror.archive.model

@Suppress("unused")
inline class Reference<MODEL : HasId>(val key: Uuid)

fun <MODEL: HasId> MODEL.reference() = Reference<MODEL>(this.id)