package com.lightningkite.mirror.archive.server

import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.MirrorClass

object SuspendProperties : SuspendProperty.Provider {
    fun configure(type: String, arguments: Map<String, String>, vararg options: SuspendProperty.Provider.FromConfiguration) {
        for (option in options) {
            if (option.name.toLowerCase() == type) {
                provider = option.invoke(arguments)//config.map("database", *(option.requiredArguments + option.optionalArguments)))
                return
            }
        }
    }

    lateinit var provider: SuspendProperty.Provider

    override fun <T : Any> get(mirrorClass: MirrorClass<T>, name: String, default: T): SuspendProperty<T> {
        return provider.get(mirrorClass, name, default)
    }
}