package com.lightningkite.mirror.archive.server

import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.property.SuspendProperty
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun ApplicationConfig.map(path: String, vararg expected: String): Map<String, String> {
    return this.config(path).let { config ->
        expected.mapNotNull { key ->
            val value = config.propertyOrNull(key)?.getString()
            if (value == null) null else key to value
        }.associate { it }
    }
}

@KtorExperimentalAPI
fun ConfiguredDatabaseProvider.configure(name: String = "database", config: ApplicationConfig) {
    configure(
            type = config.property("$name.type").getString(),
            arguments = config.map(name, *(options.flatMap { option ->
                (option.requiredArguments + option.optionalArguments).toList()
            }.toTypedArray()))
    )
}

@KtorExperimentalAPI
fun ConfiguredSuspendPropertyProvider.configure(name: String = "suspendProperty", config: ApplicationConfig) {
    configure(
            type = config.property("$name.type").getString(),
            arguments = config.map(name, *(options.flatMap { option ->
                (option.requiredArguments + option.optionalArguments).toList()
            }.toTypedArray()))
    )
}