//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

object OperationAddIntMirror : MirrorClass<Operation.AddInt>() {
    override val empty: Operation.AddInt get() = Operation.AddInt(
        amount = IntMirror.empty
    )
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.AddInt> get() = Operation.AddInt::class as KClass<Operation.AddInt>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.AddInt"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationAddNumericMirror(IntMirror))
    override val owningClass: KClass<*>? get() = Operation::class
    
    val fieldAmount: Field<Operation.AddInt,Int> = Field(
        owner = this,
        index = 0,
        name = "amount",
        type = IntMirror,
        optional = false,
        get = { it.amount },
        set = { it, value -> it.amount = value },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Operation.AddInt, *>> = arrayOf(fieldAmount)
    
    override fun deserialize(decoder: Decoder): Operation.AddInt {
        var amountSet = false
        var fieldAmount: Int? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldAmount = decoderStructure.decodeIntElement(this, 0)
                    amountSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldAmount = decoderStructure.decodeIntElement(this, 0)
                    amountSet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!amountSet) {
            throw MissingFieldException("amount")
        }
        return Operation.AddInt(
            amount = fieldAmount as Int
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Operation.AddInt) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeIntElement(this, 0, obj.amount)
        encoderStructure.endStructure(this)
    }
}
