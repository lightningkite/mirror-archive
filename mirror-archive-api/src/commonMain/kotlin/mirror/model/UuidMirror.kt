//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import kotlin.random.Random
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

object UuidMirror : MirrorClass<Uuid>() {
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Uuid>
        get() = Uuid::class as KClass<Uuid>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Uuid"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(ComparableMirror(UuidMirror))
    override val companion: Any? get() = Uuid.Companion

    val fieldMostSignificantBits: Field<Uuid, Long> = Field(
            owner = this,
            name = "mostSignificantBits",
            type = LongMirror,
            optional = false,
            get = { it.mostSignificantBits },
            annotations = listOf<Annotation>()
    )

    val fieldLeastSignificantBits: Field<Uuid, Long> = Field(
            owner = this,
            name = "leastSignificantBits",
            type = LongMirror,
            optional = false,
            get = { it.leastSignificantBits },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<Uuid, *>> = arrayOf(fieldMostSignificantBits, fieldLeastSignificantBits)

    override fun deserialize(decoder: Decoder): Uuid {
        var mostSignificantBitsSet = false
        var fieldMostSignificantBits: Long? = null
        var leastSignificantBitsSet = false
        var fieldLeastSignificantBits: Long? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldMostSignificantBits = decoderStructure.decodeLongElement(this, 0)
                    mostSignificantBitsSet = true
                    fieldLeastSignificantBits = decoderStructure.decodeLongElement(this, 1)
                    leastSignificantBitsSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldMostSignificantBits = decoderStructure.decodeLongElement(this, 0)
                    mostSignificantBitsSet = true
                }
                1 -> {
                    fieldLeastSignificantBits = decoderStructure.decodeLongElement(this, 1)
                    leastSignificantBitsSet = true
                }
                else -> {
                }
            }
        }
        decoderStructure.endStructure(this)
        if (!mostSignificantBitsSet) {
            throw MissingFieldException("mostSignificantBits")
        }
        if (!leastSignificantBitsSet) {
            throw MissingFieldException("leastSignificantBits")
        }
        return Uuid(
                mostSignificantBits = fieldMostSignificantBits as Long,
                leastSignificantBits = fieldLeastSignificantBits as Long
        )
    }

    override fun serialize(encoder: Encoder, obj: Uuid) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeLongElement(this, 0, obj.mostSignificantBits)
        encoderStructure.encodeLongElement(this, 1, obj.leastSignificantBits)
        encoderStructure.endStructure(this)
    }
}
