//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

data class ReferenceMirror<MODEL: HasUuid>(
    val MODELMirror: MirrorType<MODEL>
) : MirrorClass<Reference<MODEL>>() {
    
    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion
    companion object : MirrorClassCompanion {
        val MODELMirrorMinimal get() = HasUuidMirror
        
        override val minimal = ReferenceMirror(TypeArgumentMirrorType("MODEL", Variance.INVARIANT, MODELMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = ReferenceMirror(typeArguments[0] as MirrorType<HasUuid>)
        
        @Suppress("UNCHECKED_CAST")
        fun make(
            MODELMirror: MirrorType<*>? = null
        ) = ReferenceMirror<HasUuid>(
            MODELMirror = (MODELMirror ?: MODELMirrorMinimal) as MirrorType<HasUuid>
        )
    }
    
    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(MODELMirror)
    override val empty: Reference<MODEL> get() = Reference(
        key = UuidMirror.empty
    )
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Reference<MODEL>> get() = Reference::class as KClass<Reference<MODEL>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Inline)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Reference"
    override val implements: Array<MirrorClass<*>> get() = arrayOf()
    
    val fieldKey: Field<Reference<MODEL>,Uuid> = Field(
        owner = this,
        index = 0,
        name = "key",
        type = UuidMirror,
        optional = false,
        get = { it.key },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Reference<MODEL>, *>> = arrayOf(fieldKey)
    
    override fun deserialize(decoder: Decoder): Reference<MODEL> {
        var keySet = false
        var fieldKey: Uuid? = null
        val decoderStructure = decoder.beginStructure(this, MODELMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldKey = decoderStructure.decodeSerializableElement(this, 0, UuidMirror)
                    keySet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldKey = decoderStructure.decodeSerializableElement(this, 0, UuidMirror)
                    keySet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!keySet) {
            throw MissingFieldException("key")
        }
        return Reference<MODEL>(
            key = fieldKey as Uuid
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Reference<MODEL>) {
        val encoderStructure = encoder.beginStructure(this, MODELMirror)
        encoderStructure.encodeSerializableElement(this, 0, UuidMirror, obj.key)
        encoderStructure.endStructure(this)
    }
}
