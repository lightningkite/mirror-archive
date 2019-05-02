package com.lightningkite.mirror.archive.model

@Suppress("unused")
inline class Reference<MODEL : HasUuid>(val key: Uuid)

fun <MODEL: HasUuid> MODEL.reference() = Reference<MODEL>(this.id)