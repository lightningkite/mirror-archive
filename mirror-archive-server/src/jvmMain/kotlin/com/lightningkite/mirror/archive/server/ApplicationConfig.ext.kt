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
fun Databases.configure(config: ApplicationConfig, vararg options: Database.Provider.FromConfiguration) {
    Databases.configure(
            type = config.property("database.type").getString(),
            arguments = config.map("database", *(options.flatMap { option ->
                (option.requiredArguments + option.optionalArguments).toList()
            }.toTypedArray())),
            options = *options
    )
}

@KtorExperimentalAPI
fun SuspendProperties.configure(config: ApplicationConfig, vararg options: SuspendProperty.Provider.FromConfiguration) {
    SuspendProperties.configure(
            type = config.property("suspendProperty.type").getString(),
            arguments = config.map("suspendProperty", *(options.flatMap { option ->
                (option.requiredArguments + option.optionalArguments).toList()
            }.toTypedArray())),
            options = *options
    )
}