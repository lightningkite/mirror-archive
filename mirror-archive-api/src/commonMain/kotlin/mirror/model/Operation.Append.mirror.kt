//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

object OperationAppendMirror : MirrorClass<Operation.Append>() {
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.Append> get() = Operation.Append::class as KClass<Operation.Append>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.Append"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationMirror(StringMirror))
    override val owningClass: KClass<*>? get() = Operation::class
    
    val fieldString: Field<Operation.Append,String> = Field(
        owner = this,
        index = 0,
        name = "string",
        type = StringMirror,
        optional = false,
        get = { it.string },
        set = { it, value -> it.string = value },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Operation.Append, *>> = arrayOf(fieldString)
    
    override fun deserialize(decoder: Decoder): Operation.Append {
        var stringSet = false
        var fieldString: String? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldString = decoderStructure.decodeStringElement(this, 0)
                    stringSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldString = decoderStructure.decodeStringElement(this, 0)
                    stringSet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!stringSet) {
            throw MissingFieldException("string")
        }
        return Operation.Append(
            string = fieldString as String
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Operation.Append) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeStringElement(this, 0, obj.string)
        encoderStructure.endStructure(this)
    }
}
