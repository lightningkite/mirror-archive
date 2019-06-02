package com.lightningkite.mirror.archive.property

import com.lightningkite.kommon.atomic.AtomicValue
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import kotlinx.serialization.json.Json
import java.io.File
import java.lang.Exception


class FileSuspendProperty<T>(val file: File, val type: MirrorType<T>, val default: T) : SuspendProperty<T> {
    val ramBacking = AtomicValue<T?>(null)

    init {
        if(!file.parentFile.exists()){
            file.parentFile.mkdirs()
        }
    }

    companion object FromConfiguration : SuspendProperty.Provider.FromConfiguration {
        override val name: String get() = "File"
        override val optionalArguments: Array<String>
            get() = arrayOf("files")
        override fun invoke(arguments: Map<String, String>) = Provider(File(arguments["files"] ?: "."))
    }

    class Provider(val baseFolder: File) : SuspendProperty.Provider {
        override fun <T : Any> get(mirrorClass: MirrorClass<T>, name: String, default: T): SuspendProperty<T> {
            return FileSuspendProperty(File(baseFolder, name), mirrorClass, default)
        }
    }

    suspend fun load(): T {
        val parsed = try {
            Json.parse(type, file.readText())
        } catch(e:Exception){
            set(default)
            default
        }
        ramBacking.value = parsed
        return parsed
    }

    override suspend fun get(): T = ramBacking.value ?: load()

    override suspend fun set(value: T) {
        ramBacking.value = value
        file.writeText(Json.stringify(type, value))
    }

    override suspend fun compareAndSet(expected: T, value: T): Boolean {
        if(ramBacking.value == null) load()
        if(ramBacking.compareAndSet(expected, value)){
            file.writeText(Json.stringify(type, value))
            return true
        } else {
            return false
        }
    }
}