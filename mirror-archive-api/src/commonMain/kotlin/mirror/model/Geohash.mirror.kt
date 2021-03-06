//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.lokalize.location

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*
import com.lightningkite.lokalize.location.GeohashMirror

object GeohashMirror : MirrorClass<Geohash>() {
    override val empty: Geohash get() = Geohash(
        bits = LongMirror.empty
    )
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Geohash> get() = Geohash::class as KClass<Geohash>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Inline)
    override val packageName: String get() = "com.lightningkite.lokalize.location"
    override val localName: String get() = "Geohash"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(ComparableMirror(com.lightningkite.lokalize.location.GeohashMirror))
    override val companion: Any? get() = Geohash.Companion
    
    val fieldBits: Field<Geohash,kotlin.Long> = Field(
        owner = this,
        index = 0,
        name = "bits",
        type = LongMirror,
        optional = false,
        get = { it.bits },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Geohash, *>> = arrayOf(fieldBits)
    
    override fun deserialize(decoder: Decoder): Geohash {
        var bitsSet = false
        var fieldBits: kotlin.Long? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldBits = decoderStructure.decodeLongElement(this, 0)
                    bitsSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldBits = decoderStructure.decodeLongElement(this, 0)
                    bitsSet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!bitsSet) {
            throw MissingFieldException("bits")
        }
        return Geohash(
            bits = fieldBits as kotlin.Long
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Geohash) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeLongElement(this, 0, obj.bits)
        encoderStructure.endStructure(this)
    }
}
