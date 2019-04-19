//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

class OperationMultipleMirror<T : Any?>(
        val TMirror: MirrorType<T>
) : MirrorClass<Operation.Multiple<T>>() {

    companion object {
        val minimal = OperationMultipleMirror(AnyMirror.nullable)
    }

    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.Multiple<T>>
        get() = Operation.Multiple::class as KClass<Operation.Multiple<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.Multiple"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationMirror(TMirror))
    override val owningClass: KClass<*>? get() = Operation::class

    val fieldOperations: Field<Operation.Multiple<T>, List<Operation<T>>> = Field(
            owner = this,
            index = 0,
            name = "operations",
            type = ListMirror(OperationMirror(TMirror)),
            optional = false,
            get = { it.operations },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<Operation.Multiple<T>, *>> = arrayOf(fieldOperations)

    override fun deserialize(decoder: Decoder): Operation.Multiple<T> {
        var operationsSet = false
        var fieldOperations: List<Operation<T>>? = null
        val decoderStructure = decoder.beginStructure(this, TMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldOperations = decoderStructure.decodeSerializableElement(this, 0, ListMirror(OperationMirror(TMirror)))
                    operationsSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldOperations = decoderStructure.decodeSerializableElement(this, 0, ListMirror(OperationMirror(TMirror)))
                    operationsSet = true
                }
                else -> {
                }
            }
        }
        decoderStructure.endStructure(this)
        if (!operationsSet) {
            throw MissingFieldException("operations")
        }
        return Operation.Multiple<T>(
                operations = fieldOperations as List<Operation<T>>
        )
    }

    override fun serialize(encoder: Encoder, obj: Operation.Multiple<T>) {
        val encoderStructure = encoder.beginStructure(this, TMirror)
        encoderStructure.encodeSerializableElement(this, 0, ListMirror(OperationMirror(TMirror)), obj.operations)
        encoderStructure.endStructure(this)
    }
}
