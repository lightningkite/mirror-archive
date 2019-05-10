package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass


data class TypedReference(val type: MirrorClass<*>, val key: Uuid)

fun <MODEL: HasUuid> MODEL.typedReference(type: MirrorClass<MODEL>) = TypedReference(type, this.id)