package com.lightningkite.mirror.archive.server

import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.MirrorClass

class ConfiguredSuspendPropertyProvider(vararg val options: SuspendProperty.Provider.FromConfiguration) : SuspendProperty.Provider {
    fun configure(type: String, arguments: Map<String, String>) {
        for (option in options) {
            if (option.name.toLowerCase() == type.toLowerCase()) {
                provider = option.invoke(arguments)//config.map("database", *(option.requiredArguments + option.optionalArguments)))
                return
            }
        }
        println("No matching provider type $type")
    }

    lateinit var provider: SuspendProperty.Provider

    override fun <T : Any> get(mirrorClass: MirrorClass<T>, name: String, default: T): SuspendProperty<T> {
        return provider.get(mirrorClass, name, default)
    }
}