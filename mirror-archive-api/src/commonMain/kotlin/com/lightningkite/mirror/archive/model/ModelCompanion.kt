package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass

interface ModelCompanion<T: Any> {
    val mirrorClass: MirrorClass<T>
    val tableName: String get() = mirrorClass.localName
    val empty: T
}