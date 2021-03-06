//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

data class OperationAddNumericMirror<T: Any?>(
    val TMirror: MirrorType<T>
) : PolymorphicMirror<Operation.AddNumeric<T>>() {
    
    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion
    companion object : MirrorClassCompanion {
        val TMirrorMinimal get() = AnyMirror.nullable
        
        override val minimal = OperationAddNumericMirror(TypeArgumentMirrorType("T", Variance.INVARIANT, TMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = OperationAddNumericMirror(typeArguments[0] as MirrorType<Any?>)
        
        @Suppress("UNCHECKED_CAST")
        fun make(
            TMirror: MirrorType<*>? = null
        ) = OperationAddNumericMirror<Any?>(
            TMirror = (TMirror ?: TMirrorMinimal) as MirrorType<Any?>
        )
    }
    
    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.AddNumeric<T>> get() = Operation.AddNumeric::class as KClass<Operation.AddNumeric<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Interface)
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationMirror(TMirror))
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.AddNumeric"
    override val owningClass: KClass<*>? get() = Operation::class
}
