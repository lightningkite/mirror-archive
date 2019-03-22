//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

class SortMirror<T : Any, V : Comparable<V>>(
        val TMirror: MirrorType<T>,
        val VMirror: MirrorType<V>
) : MirrorClass<Sort<T, V>>() {
    
    companion object {
        val minimal = SortMirror(AnyMirror, ComparableMirror(ComparableMirror(ComparableMirror(AnyMirror.nullable))))
    }

    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror, VMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Sort<T, V>>
        get() = Sort::class as KClass<Sort<T, V>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Sort"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(ComparatorMirror(TMirror))

    val fieldField: Field<Sort<T, V>, MirrorClass.Field<T, V>> = Field(
            owner = this,
            index = 0,
            name = "field",
            type = MirrorClassFieldMirror(TMirror, VMirror),
            optional = false,
            get = { it.field },
            annotations = listOf<Annotation>()
    )

    val fieldAscending: Field<Sort<T, V>, Boolean> = Field(
            owner = this,
            index = 1,
            name = "ascending",
            type = BooleanMirror,
            optional = true,
            get = { it.ascending },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<Sort<T, V>, *>> = arrayOf(fieldField, fieldAscending)

    override fun deserialize(decoder: Decoder): Sort<T, V> {
        var fieldSet = false
        var fieldField: MirrorClass.Field<T, V>? = null
        var ascendingSet = false
        var fieldAscending: Boolean? = null
        val decoderStructure = decoder.beginStructure(this, TMirror, VMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldField = decoderStructure.decodeSerializableElement(this, 0, MirrorClassFieldMirror(TMirror, VMirror))
                    fieldSet = true
                    fieldAscending = decoderStructure.decodeBooleanElement(this, 1)
                    ascendingSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldField = decoderStructure.decodeSerializableElement(this, 0, MirrorClassFieldMirror(TMirror, VMirror))
                    fieldSet = true
                }
                1 -> {
                    fieldAscending = decoderStructure.decodeBooleanElement(this, 1)
                    ascendingSet = true
                }
                else -> {
                }
            }
        }
        decoderStructure.endStructure(this)
        if (!fieldSet) {
            throw MissingFieldException("field")
        }
        if (!ascendingSet) {
            fieldAscending = true
        }
        return Sort<T, V>(
                field = fieldField as MirrorClass.Field<T, V>,
                ascending = fieldAscending as Boolean
        )
    }

    override fun serialize(encoder: Encoder, obj: Sort<T, V>) {
        val encoderStructure = encoder.beginStructure(this, TMirror, VMirror)
        encoderStructure.encodeSerializableElement(this, 0, MirrorClassFieldMirror(TMirror, VMirror), obj.field)
        encoderStructure.encodeBooleanElement(this, 1, obj.ascending)
        encoderStructure.endStructure(this)
    }
}
