//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass
import kotlinx.serialization.Mapper
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

class OperationSetMirror<T : Any?>(
        val TMirror: MirrorType<T>
) : MirrorClass<Operation.Set<T>>() {

    companion object {
        val minimal = OperationSetMirror(AnyMirror.nullable)
    }

    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.Set<T>>
        get() = Operation.Set::class as KClass<Operation.Set<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.Set"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationMirror(TMirror))
    override val owningClass: KClass<*>? get() = Operation::class

    val fieldValue: Field<Operation.Set<T>, T> = Field(
            owner = this,
            name = "value",
            type = TMirror,
            optional = false,
            get = { it.value },
            set = { it, value -> it.value = value },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<Operation.Set<T>, *>> = arrayOf(fieldValue)

    override fun deserialize(decoder: Decoder): Operation.Set<T> {
        var valueSet = false
        var fieldValue: T? = null
        val decoderStructure = decoder.beginStructure(this, TMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldValue = decoderStructure.decodeSerializableElement(this, 0, TMirror)
                    valueSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldValue = decoderStructure.decodeSerializableElement(this, 0, TMirror)
                    valueSet = true
                }
                else -> {
                }
            }
        }
        decoderStructure.endStructure(this)
        if (!valueSet) {
            throw MissingFieldException("value")
        }
        return Operation.Set<T>(
                value = fieldValue as T
        )
    }

    override fun serialize(encoder: Encoder, obj: Operation.Set<T>) {
        val encoderStructure = encoder.beginStructure(this, TMirror)
        encoderStructure.encodeSerializableElement(this, 0, TMirror, obj.value)
        encoderStructure.endStructure(this)
    }
}
