//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

object HasUuidMirror : PolymorphicMirror<HasUuid>() {
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<HasUuid> get() = HasUuid::class as KClass<HasUuid>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Interface)
    override val implements: Array<MirrorClass<*>> get() = arrayOf(HasIdMirror(UuidMirror))
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "HasUuid"
}
