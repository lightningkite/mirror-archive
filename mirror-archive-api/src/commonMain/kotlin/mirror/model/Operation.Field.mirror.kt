//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.breaker.Breaker
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*
import com.lightningkite.mirror.info.MirrorClassFieldMirror

data class OperationFieldMirror<T: Any, V: Any?>(
    val TMirror: MirrorType<T>,
    val VMirror: MirrorType<V>
) : MirrorClass<Operation.Field<T,V>>() {
    
    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion
    companion object : MirrorClassCompanion {
        val TMirrorMinimal get() = AnyMirror
        val VMirrorMinimal get() = AnyMirror.nullable
        
        override val minimal = OperationFieldMirror(TypeArgumentMirrorType("T", Variance.INVARIANT, TMirrorMinimal), TypeArgumentMirrorType("V", Variance.INVARIANT, VMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = OperationFieldMirror(typeArguments[0] as MirrorType<Any>, typeArguments[1] as MirrorType<Any?>)
        
        @Suppress("UNCHECKED_CAST")
        fun make(
            TMirror: MirrorType<*>? = null,
            VMirror: MirrorType<*>? = null
        ) = OperationFieldMirror<Any, Any?>(
            TMirror = (TMirror ?: TMirrorMinimal) as MirrorType<Any>,
            VMirror = (VMirror ?: VMirrorMinimal) as MirrorType<Any?>
        )
    }
    
    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror, VMirror)
    override val empty: Operation.Field<T,V> get() = Operation.Field(
        field = MirrorClassFieldMirror(TMirror, VMirror).empty,
        operation = OperationMirror(VMirror).empty
    )
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Operation.Field<T,V>> get() = Operation.Field::class as KClass<Operation.Field<T,V>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Operation.Field"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(OperationMirror(TMirror))
    override val owningClass: KClass<*>? get() = Operation::class
    
    val fieldField: Field<Operation.Field<T,V>,MirrorClass.Field<T, V>> = Field(
        owner = this,
        index = 0,
        name = "field",
        type = MirrorClassFieldMirror(TMirror, VMirror),
        optional = false,
        get = { it.field },
        annotations = listOf<Annotation>()
    )
    
    val fieldOperation: Field<Operation.Field<T,V>,Operation<V>> = Field(
        owner = this,
        index = 1,
        name = "operation",
        type = OperationMirror(VMirror),
        optional = false,
        get = { it.operation },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Operation.Field<T,V>, *>> = arrayOf(fieldField, fieldOperation)
    
    override fun deserialize(decoder: Decoder): Operation.Field<T,V> {
        var fieldSet = false
        var fieldField: MirrorClass.Field<T, V>? = null
        var operationSet = false
        var fieldOperation: Operation<V>? = null
        val decoderStructure = decoder.beginStructure(this, TMirror, VMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldField = decoderStructure.decodeSerializableElement(this, 0, MirrorClassFieldMirror(TMirror, VMirror))
                    fieldSet = true
                    fieldOperation = decoderStructure.decodeSerializableElement(this, 1, OperationMirror(VMirror))
                    operationSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldField = decoderStructure.decodeSerializableElement(this, 0, MirrorClassFieldMirror(TMirror, VMirror))
                    fieldSet = true
                }
                1 -> {
                    fieldOperation = decoderStructure.decodeSerializableElement(this, 1, OperationMirror(VMirror))
                    operationSet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!fieldSet) {
            throw MissingFieldException("field")
        }
        if(!operationSet) {
            throw MissingFieldException("operation")
        }
        return Operation.Field<T,V>(
            field = fieldField as MirrorClass.Field<T, V>,
            operation = fieldOperation as Operation<V>
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Operation.Field<T,V>) {
        val encoderStructure = encoder.beginStructure(this, TMirror, VMirror)
        encoderStructure.encodeSerializableElement(this, 0, MirrorClassFieldMirror(TMirror, VMirror), obj.field)
        encoderStructure.encodeSerializableElement(this, 1, OperationMirror(VMirror), obj.operation)
        encoderStructure.endStructure(this)
    }
}
