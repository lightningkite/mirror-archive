//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

object OperationAddLongMirror : MirrorClass<Operation.AddLong>() {
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.AddLong>
        get() = Operation.AddLong::class as KClass<Operation.AddLong>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.AddLong"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationAddNumericMirror(LongMirror))
    override val owningClass: KClass<*>? get() = Operation::class

    val fieldAmount: Field<Operation.AddLong, Long> = Field(
            owner = this,
            index = 0,
            name = "amount",
            type = LongMirror,
            optional = false,
            get = { it.amount },
            set = { it, value -> it.amount = value },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<Operation.AddLong, *>> = arrayOf(fieldAmount)

    override fun deserialize(decoder: Decoder): Operation.AddLong {
        var amountSet = false
        var fieldAmount: Long? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldAmount = decoderStructure.decodeLongElement(this, 0)
                    amountSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldAmount = decoderStructure.decodeLongElement(this, 0)
                    amountSet = true
                }
                else -> {
                }
            }
        }
        decoderStructure.endStructure(this)
        if (!amountSet) {
            throw MissingFieldException("amount")
        }
        return Operation.AddLong(
                amount = fieldAmount as Long
        )
    }

    override fun serialize(encoder: Encoder, obj: Operation.AddLong) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeLongElement(this, 0, obj.amount)
        encoderStructure.endStructure(this)
    }
}