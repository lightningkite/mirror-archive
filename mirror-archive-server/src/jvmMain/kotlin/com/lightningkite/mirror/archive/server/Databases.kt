package com.lightningkite.mirror.archive.server

import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.info.MirrorClass

object Databases : Database.Provider {
    fun configure(type: String, arguments: Map<String, String>, vararg options: Database.Provider.FromConfiguration) {
        for (option in options) {
            if (option.name.toLowerCase() == type) {
                provider = option.invoke(arguments)//config.map("database", *(option.requiredArguments + option.optionalArguments)))
                return
            }
        }
    }

    lateinit var provider: Database.Provider

    override fun <T : Any> get(mirrorClass: MirrorClass<T>, default: T, name: String): Database<T> = provider.get(mirrorClass, default, name)
}