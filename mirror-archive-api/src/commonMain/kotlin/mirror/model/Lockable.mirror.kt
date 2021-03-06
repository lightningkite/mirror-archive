//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

data class LockableMirror<ID: Any?>(
    val IDMirror: MirrorType<ID>
) : PolymorphicMirror<Lockable<ID>>() {
    
    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion
    companion object : MirrorClassCompanion {
        val IDMirrorMinimal get() = AnyMirror.nullable
        
        override val minimal = LockableMirror(TypeArgumentMirrorType("ID", Variance.INVARIANT, IDMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = LockableMirror(typeArguments[0] as MirrorType<Any?>)
        
        @Suppress("UNCHECKED_CAST")
        fun make(
            IDMirror: MirrorType<*>? = null
        ) = LockableMirror<Any?>(
            IDMirror = (IDMirror ?: IDMirrorMinimal) as MirrorType<Any?>
        )
    }
    
    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(IDMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Lockable<ID>> get() = Lockable::class as KClass<Lockable<ID>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Interface)
    override val implements: Array<MirrorClass<*>> get() = arrayOf(HasIdMirror(IDMirror))
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Lockable"
}
