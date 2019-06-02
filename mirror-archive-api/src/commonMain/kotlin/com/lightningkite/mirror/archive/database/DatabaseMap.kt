package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.info.MirrorClass

class DatabaseMap(val map: Map<MirrorClass<*>, Database<*>> = mapOf()) : Database.Provider {

    constructor(vararg pairs: Pair<MirrorClass<*>, Database<*>>):this(mapOf(*pairs))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getOrNull(mirrorClass: MirrorClass<T>): Database<T>? = map[mirrorClass] as? Database<T>
}