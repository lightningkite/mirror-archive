package com.lightningkite.mirror.archive.server

import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.info.MirrorClass

class ConfiguredDatabaseProvider(vararg val options: Database.Provider.FromConfiguration) : Database.Provider {
    fun configure(type: String, arguments: Map<String, String>) {
        for (option in options) {
            if (option.name.toLowerCase() == type.toLowerCase()) {
                provider = option.invoke(arguments)//config.map("database", *(option.requiredArguments + option.optionalArguments)))
                return
            }
        }
        println("No matching provider type $type")
    }

    lateinit var provider: Database.Provider

    override fun <T : Any> get(mirrorClass: MirrorClass<T>, default: T, name: String): Database<T> = provider.get(mirrorClass, default, name)
}