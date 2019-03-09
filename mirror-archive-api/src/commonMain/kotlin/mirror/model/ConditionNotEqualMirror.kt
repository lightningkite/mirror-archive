//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.kommon.collection.treeWalkDepthSequence
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

class ConditionNotEqualMirror<T : Any?>(
        val TMirror: MirrorType<T>
) : MirrorClass<Condition.NotEqual<T>>() {

    companion object {
        val minimal = ConditionNotEqualMirror(AnyMirror.nullable)
    }

    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Condition.NotEqual<T>>
        get() = Condition.NotEqual::class as KClass<Condition.NotEqual<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Condition.NotEqual"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(ConditionMirror(TMirror))
    override val owningClass: KClass<*>? get() = Condition::class

    val fieldValue: Field<Condition.NotEqual<T>, T> = Field(
            owner = this,
            name = "value",
            type = TMirror,
            optional = false,
            get = { it.value },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<Condition.NotEqual<T>, *>> = arrayOf(fieldValue)

    override fun deserialize(decoder: Decoder): Condition.NotEqual<T> {
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
        return Condition.NotEqual<T>(
                value = fieldValue as T
        )
    }

    override fun serialize(encoder: Encoder, obj: Condition.NotEqual<T>) {
        val encoderStructure = encoder.beginStructure(this, TMirror)
        encoderStructure.encodeSerializableElement(this, 0, TMirror, obj.value)
        encoderStructure.endStructure(this)
    }
}
