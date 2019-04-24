//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

object OperationAddDoubleMirror : MirrorClass<Operation.AddDouble>() {
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.AddDouble> get() = Operation.AddDouble::class as KClass<Operation.AddDouble>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.AddDouble"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationAddNumericMirror(DoubleMirror))
    override val owningClass: KClass<*>? get() = Operation::class
    
    val fieldAmount: Field<Operation.AddDouble,Double> = Field(
        owner = this,
        index = 0,
        name = "amount",
        type = DoubleMirror,
        optional = false,
        get = { it.amount },
        set = { it, value -> it.amount = value },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Operation.AddDouble, *>> = arrayOf(fieldAmount)
    
    override fun deserialize(decoder: Decoder): Operation.AddDouble {
        var amountSet = false
        var fieldAmount: Double? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldAmount = decoderStructure.decodeDoubleElement(this, 0)
                    amountSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldAmount = decoderStructure.decodeDoubleElement(this, 0)
                    amountSet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!amountSet) {
            throw MissingFieldException("amount")
        }
        return Operation.AddDouble(
            amount = fieldAmount as Double
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Operation.AddDouble) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeDoubleElement(this, 0, obj.amount)
        encoderStructure.endStructure(this)
    }
}
