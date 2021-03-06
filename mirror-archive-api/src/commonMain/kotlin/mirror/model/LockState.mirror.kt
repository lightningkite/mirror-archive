//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.lokalize.time.TimeStamp
import com.lightningkite.lokalize.time.now
import kotlin.random.Random
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

object LockStateMirror : MirrorClass<LockState>() {
    override val empty: LockState get() = LockState(
        value = LongMirror.empty
    )
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<LockState> get() = LockState::class as KClass<LockState>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Inline)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "LockState"
    override val implements: Array<MirrorClass<*>> get() = arrayOf()
    override val companion: Any? get() = LockState.Companion
    
    val fieldValue: Field<LockState,Long> = Field(
        owner = this,
        index = 0,
        name = "value",
        type = LongMirror,
        optional = false,
        get = { it.value },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<LockState, *>> = arrayOf(fieldValue)
    
    override fun deserialize(decoder: Decoder): LockState {
        var valueSet = false
        var fieldValue: Long? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldValue = decoderStructure.decodeLongElement(this, 0)
                    valueSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldValue = decoderStructure.decodeLongElement(this, 0)
                    valueSet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!valueSet) {
            throw MissingFieldException("value")
        }
        return LockState(
            value = fieldValue as Long
        )
    }
    
    override fun serialize(encoder: Encoder, obj: LockState) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeLongElement(this, 0, obj.value)
        encoderStructure.endStructure(this)
    }
}
