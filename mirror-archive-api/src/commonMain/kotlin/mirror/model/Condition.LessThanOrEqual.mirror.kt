//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.kommon.collection.treeWalkDepthSequence
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

data class ConditionLessThanOrEqualMirror<T: Comparable<T>?>(
    val TMirror: MirrorType<T>
) : MirrorClass<Condition.LessThanOrEqual<T>>() {
    
    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion
    companion object : MirrorClassCompanion {
        val TMirrorMinimal get() = ComparableMirror(ComparableMirror(ComparableMirror(AnyMirror.nullable).nullable).nullable).nullable
        
        override val minimal = ConditionLessThanOrEqualMirror(TypeArgumentMirrorType("T", Variance.INVARIANT, TMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = ConditionLessThanOrEqualMirror(typeArguments[0] as MirrorType<Comparable<Comparable<Comparable<Comparable<*>?>?>?>?>)

        @Suppress("UNCHECKED_CAST")
        fun make(
                TMirror: MirrorType<*>? = null
        ) = ConditionLessThanOrEqualMirror<Comparable<Comparable<Comparable<Comparable<*>?>?>?>?>(
                TMirror = (TMirror
                        ?: TMirrorMinimal) as MirrorType<Comparable<Comparable<Comparable<Comparable<*>?>?>?>?>
        )
    }
    
    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Condition.LessThanOrEqual<T>> get() = Condition.LessThanOrEqual::class as KClass<Condition.LessThanOrEqual<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Condition.LessThanOrEqual"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(ConditionMirror(TMirror))
    override val owningClass: KClass<*>? get() = Condition::class
    
    val fieldValue: Field<Condition.LessThanOrEqual<T>,T> = Field(
        owner = this,
        index = 0,
        name = "value",
        type = TMirror,
        optional = false,
        get = { it.value },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Condition.LessThanOrEqual<T>, *>> = arrayOf(fieldValue)
    
    override fun deserialize(decoder: Decoder): Condition.LessThanOrEqual<T> {
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
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!valueSet) {
            throw MissingFieldException("value")
        }
        return Condition.LessThanOrEqual<T>(
            value = fieldValue as T
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Condition.LessThanOrEqual<T>) {
        val encoderStructure = encoder.beginStructure(this, TMirror)
        encoderStructure.encodeSerializableElement(this, 0, TMirror, obj.value)
        encoderStructure.endStructure(this)
    }
}
